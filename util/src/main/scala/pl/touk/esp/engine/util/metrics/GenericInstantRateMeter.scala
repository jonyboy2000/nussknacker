package pl.touk.esp.engine.util.metrics

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder

trait RateMeter {
  def mark(): Unit
}

//To jest bardzo slaba implementacja, ale na nasze potrzeby moze wystarczy...
trait GenericInstantRateMeter extends RateMeter {

  val counter = new LongAdder
  private val NANOS_IN_SECOND = TimeUnit.SECONDS.toNanos(1)
  private val TICK_INTERVAL = TimeUnit.SECONDS.toNanos(1)
  var lastTick = System.nanoTime()

  var lastValue = 0d

  override def mark(): Unit = {
    counter.add(1)
  }

  def getValue = synchronized {
    val previousTick = lastTick
    val currentTime = System.nanoTime()
    val timeFromLast = currentTime - previousTick
    if (timeFromLast > TICK_INTERVAL) {
      lastTick = currentTime
      val count = counter.sumThenReset()
      lastValue = NANOS_IN_SECOND * count.toDouble / timeFromLast
    }
    lastValue
  }

}