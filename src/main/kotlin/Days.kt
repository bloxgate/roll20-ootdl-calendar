enum class Days(val order: Int) {
    Helios(1),
    Damona(2),
    Polemeos(3),
    Apatios(4),
    Xandos(5),
    Adonus(6),
    Thanato(7);

    companion object {
        fun getByDayOfWeek(d: Int): Days
        {
            return values().first {
                it.order == d
            }
        }
    }
}