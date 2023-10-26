package com.poiuyreq0.koko

interface OnItemClickListener {
    fun onItemClick(position: Int, dataSet: MutableMap<String, Item>)
}