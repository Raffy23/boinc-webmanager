package at.happywetter.boinc.shared

/**
  * Created by: 
  *
  * @author Raphael
  * @version 04.03.2018
  */
package object webrpc:

  case class ServerStatus(daemon_status: Seq[Daemon],
                          database_file_states: DatabaseFileStates,
                          tasks_by_app: Seq[ServerStatusApp]
  )

  case class Daemon(host: String, command: String, status: String)

  case class DatabaseFileStates(results_ready_to_send: Int,
                                results_in_progress: Int,
                                workunits_waiting_for_validation: Int,
                                workunits_waiting_for_assimilation: Int,
                                workunits_waiting_for_deletion: Int,
                                results_waiting_for_deletion: Int,
                                transitioner_backlog_hours: Double,
                                users_with_recent_credit: Int,
                                users_with_credit: Int,
                                users_registered_in_past_24_hours: Int,
                                hosts_with_recent_credit: Int,
                                hosts_with_credit: Int,
                                hosts_registered_in_past_24_hours: Int,
                                current_floating_point_speed: Double
  )

  case class ServerStatusApp(id: Int,
                             name: String,
                             unsent: Int,
                             in_progress: Int,
                             avg_runtime: Double,
                             min_runtime: Double,
                             max_runtime: Double,
                             users: Int
  )

  final case class User(username: String, passwordHash: String, nonce: String)
