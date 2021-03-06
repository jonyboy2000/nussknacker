package pl.touk.nussknacker.restmodel

import java.time.LocalDateTime

import org.scalatest.{FunSuite, Matchers}
import pl.touk.nussknacker.engine.api.StreamMetaData
import pl.touk.nussknacker.engine.graph.evaluatedparam.Parameter
import pl.touk.nussknacker.engine.graph.exceptionhandler.ExceptionHandlerRef
import pl.touk.nussknacker.engine.graph.expression.Expression
import pl.touk.nussknacker.engine.graph.node.SubprocessInputDefinition.{SubprocessClazzRef, SubprocessParameter}
import pl.touk.nussknacker.engine.graph.node.{CustomNode, SubprocessInputDefinition}
import pl.touk.nussknacker.restmodel.displayedgraph.displayablenode.{NodeAdditionalFields, ProcessAdditionalFields}
import pl.touk.nussknacker.restmodel.displayedgraph.{DisplayableProcess, ProcessProperties}
import pl.touk.nussknacker.restmodel.processdetails.{DeploymentEntry, ProcessHistoryEntry}

class RestModelCodecsSpec extends FunSuite with Matchers {


  test("displayable process encode and decode") {
    val process = DisplayableProcess("", ProcessProperties(
      StreamMetaData(), ExceptionHandlerRef(List()),
      false,
      Some(ProcessAdditionalFields(Some("a"), Set(), Map("field1" -> "value1"))), Map()
    ), List(
      SubprocessInputDefinition("proc1", List(SubprocessParameter("param1", SubprocessClazzRef[String]))),
      CustomNode("id", Some("out1"), "typ1", List(Parameter("name1", Expression("spel", "11"))),
        Some(NodeAdditionalFields(Some("desc"))))
    ), List(

    ), "")

    val encoded = RestModelCodecs.displayableProcessCodec.encode(process)


    RestModelCodecs.displayableProcessCodec.decodeJson(encoded).toOption shouldBe Some(process)
  }

  test("process history encode") {

    RestModelCodecs.processHistoryEncode.encode(ProcessHistoryEntry("id", "name", 10, LocalDateTime.now(), "", List(
      DeploymentEntry(12, "env", LocalDateTime.now(), "user", Map("key" -> "value"))
    )))
  }

}
