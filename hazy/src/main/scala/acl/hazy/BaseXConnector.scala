package acl.hazy

trait BaseXConnector[RequestType] {
  def httpUri: String
  def auth(request: RequestType): RequestType
}
