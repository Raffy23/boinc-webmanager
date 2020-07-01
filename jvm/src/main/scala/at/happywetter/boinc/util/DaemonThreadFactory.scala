package at.happywetter.boinc.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by: 
 *
 * @author Raphael
 * @version 30.06.2020
 */
class DaemonThreadFactory(name: String) extends ThreadFactory {

  private val threadID = new AtomicInteger(0)

  override def newThread(r: Runnable): Thread = {
    val t = new Thread(r)
    t.setName(s"$name-${threadID.getAndAdd(1)}")
    t.setDaemon(true)

    t
  }

}
