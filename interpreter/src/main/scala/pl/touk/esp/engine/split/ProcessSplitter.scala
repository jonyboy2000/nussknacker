package pl.touk.esp.engine.split


import pl.touk.esp.engine.graph.EspProcess
import pl.touk.esp.engine.graph.node._
import pl.touk.esp.engine.splittedgraph._
import pl.touk.esp.engine.splittedgraph.part._
import pl.touk.esp.engine.splittedgraph.splittednode.{AggregateTrigger, NextNode, PartRef}

object ProcessSplitter {

  def split(process: EspProcess): SplittedProcess = {
    SplittedProcess(process.metaData, split(process.root))
  }

  private def split(node: Source): SourcePart = {
    val nextWithParts = traverse(node.next)
    SourcePart(node.id, node.ref, splittednode.Source(node.id, nextWithParts.next), nextWithParts.nextParts)
  }

  private def split(node: Aggregate): AggregateDefinitionPart = {
    val nextWithParts = traverse(node.next)
    val afterAggregation = AfterAggregationPart(
      id = node.id,
      aggregatedVar = node.aggregatedVar,
      next = nextWithParts.next,
      nextParts = nextWithParts.nextParts
    )
    val aggregateTriggerPart =
      //TODO: jakie id nadawac???
      AggregateTriggerPart(id = node.id, aggregate = AggregateTrigger(s"${node.id}-trigger",
        node.triggerExpression, node.foldingFunRef,
        PartRef(afterAggregation.id)),
        aggregatedVar = node.aggregatedVar,
        afterAggregation)

    AggregateDefinitionPart(
      id = node.id,
      durationInMillis = node.durationInMillis,
      slideInMillis = node.stepInMillis,
      aggregate = splittednode.AggregateDefinition(node.id, node.keyExpression, PartRef(afterAggregation.id)),
      nextPart = aggregateTriggerPart
    )
  }

  private def split(node: Sink): SinkPart = {
    SinkPart(node.id, node.ref, splittednode.Sink(node.id, node.endResult))
  }

  private def traverse(node: Node): NextWithParts =
    node match {
      case source: Source =>
        throw new IllegalArgumentException("Source shouldn't be traversed")
      case VariableBuilder(id, varName, fields, next) =>
        traverse(next).map { nextT =>
          NextNode(splittednode.VariableBuilder(id, varName, fields, nextT))
        }
      case Processor(id, service, next) =>
        traverse(next).map { nextT =>
          NextNode(splittednode.Processor(id, service, nextT))
        }
      case Enricher(id, service, output, next) =>
        traverse(next).map { nextT =>
          NextNode(splittednode.Enricher(id, service, output, nextT))
        }
      case Filter(id, expression, nextTrue, nextFalse) =>
        val nextTrueT = traverse(nextTrue)
        nextFalse.map(traverse) match {
          case Some(nextFalseT) =>
            NextWithParts(
              NextNode(splittednode.Filter(id, expression, nextTrueT.next, Some(nextFalseT.next))),
              nextTrueT.nextParts ::: nextFalseT.nextParts
            )
          case None =>
            NextWithParts(
              NextNode(splittednode.Filter(id, expression, nextTrueT.next, None)),
              nextTrueT.nextParts
            )
        }
      case Switch(id, expression, exprVal, nexts, defaultNext) =>
        val (nextsT, casesNextParts) = nexts.map { casee =>
          val nextWithParts = traverse(casee.node)
          (splittednode.Case(casee.expression, nextWithParts.next), nextWithParts.nextParts)
        }.unzip
        defaultNext.map(traverse) match {
          case Some(defaultNextT) =>
            NextWithParts(
              NextNode(splittednode.Switch(id, expression, exprVal, nextsT, Some(defaultNextT.next))),
              defaultNextT.nextParts ::: casesNextParts.flatten
            )
          case None =>
            NextWithParts(
              NextNode(splittednode.Switch(id, expression, exprVal, nextsT, None)),
              casesNextParts.flatten
            )
        }
      case sink: Sink =>
        val part = split(sink)
        NextWithParts(PartRef(part.id), List(part))
      case aggregate: Aggregate =>
        val part = split(aggregate)
        NextWithParts(PartRef(part.id), List(part))
    }

  case class NextWithParts(next: splittednode.Next, nextParts: List[SubsequentPart]) {

    def map(f: splittednode.Next => splittednode.Next): NextWithParts = {
      NextWithParts(f(next), nextParts)
    }

  }

}