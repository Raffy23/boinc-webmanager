package at.happywetter.boinc.web.model

import org.scalajs.dom
import org.scalajs.dom.Event
import scala.xml.Text
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import at.happywetter.boinc.shared.rpc.HostDetails
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.pages.component.DataTable.{IntegerColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.{ConfirmDialog, EditHostDetailsDialog}
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._

import mhtml.Var

/**
 * Created by: 
 *
 * @author Raphael
 * @version 09.07.2020
 */
object HostDetailsTableModel extends TableModel[HostDetails, HostDetailsTableRow]:

  override val header: List[(String, Boolean)] = List(
    ("name".localize, true),
    ("address".localize, true),
    ("port".localize, true),
    ("added_by".localize, true),
    ("connection_errors".localize, true),
    ("", false)
  )

  override def convert(data: List[HostDetails]): List[HostDetailsTableRow] =
    data.map(new HostDetailsTableRow(_))

class HostDetailsTableRow(details: HostDetails) extends DataTable.TableRow:

  protected val jsEditAction: Event => Unit = event => {
    EditHostDetailsDialog(this).renderToBody().show()
  }

  protected val jsDeleteAction: Event => Unit = event => {
    new ConfirmDialog(
      "delete_host".localize,
      List(Text("delete_host_dialog_body".localize)),
      _ => {
        NProgress.start()
        ClientManager
          .removeClient(details.name)
          .foreach(succ => {
            NProgress.done(true)

            if (succ) this.weakTableRef.reactiveData.update(_.filterNot(_ == this))
            else dom.window.alert("Error: Could not delete Host!")
          })
      },
      _ => { /* Do nothing */ }
    ).renderToBody().show()
  }

  protected val jsDirectConnect: Event => Unit = event => {
    dom.window.alert("Action not Implemented!")
  }

  val name = Var(details.name)
  val address = Var(details.address)
  val port = Var(details.port)
  val addedBy = Var(details.addedBy)
  val password = Var(details.password)

  override val columns: List[DataTable.TableColumn] = List(
    new StringColumn(name),
    new StringColumn(address),
    new IntegerColumn(port),
    new StringColumn(addedBy),
    new IntegerColumn(Var(details.errors)),
    new TableColumn(
      Var(
        <div>
        {
          Tooltip(Var("edit".localize),
                  <a onclick={jsEditAction}>
              <i class="fas fa-edit"></i>
            </a>
          )
        }
        {
          Tooltip(Var("direct_connection".localize),
                  <a onclick={jsDirectConnect}>
              <i class="fas fa-link"></i>
            </a>
          )
        }
        {
          Tooltip(Var("delete".localize),
                  <a onclick={jsDeleteAction}>
              <i class="fas fa-trash"></i>
            </a>
          )
        }
      </div>
      ),
      this
    ) {
      override def compare(that: TableColumn): Int = 0
    }
  )
