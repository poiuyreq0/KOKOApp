package com.poiuyreq0.koko

data class Cafe(
    val id: Long,
    val name: String,
    val coordinate: Coordinate,
    val positions: List<Long>,
    val benefits: Map<String, Long>
)
