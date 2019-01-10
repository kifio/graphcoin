package imurashov.graphcoin.repository.network

data class Response(val status: String,
                    val name: String?,
                    val unit: String?,
                    val period: String?,
                    val description: String,
                    val values: List<Value>) {
    data class Value(val x: Long, val y: Double)
}