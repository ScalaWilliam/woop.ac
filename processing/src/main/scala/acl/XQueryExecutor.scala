package acl

import scala.concurrent.Future

trait XQueryExecutor {
  def databaseName: String
  def queryXmlAsync(query: scala.xml.Elem): Future[scala.xml.Elem]
  def queryAsync(query: scala.xml.Elem): Future[String]
  def queryXml(query: scala.xml.Elem): scala.xml.Elem
  def query(query: scala.xml.Elem): String
}