package com.poiuyreq0.koko

class NaverMapAPIResult {
    data class ResultResponse(
        val code: Int,
        val message: String,
        val currentDateTime: String,
        val route: ResultRoute,
    )
    data class ResultRoute(
        val traoptimal: List<ResultTraoptimal>
    )
    data class ResultTraoptimal(
        val summary: ResultSummary,
        val path: List<List<Double>>
    )
    data class ResultSummary(
        val distance: Int,
        val duration: Int
    )
}