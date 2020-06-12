enum class Month(val order: Int, val days: Int) {
    Thylion(1, 30),
    Mytrion(2, 29),
    Sydion(3, 30),
    Volkion(4 , 29),
    Kention(5, 30),
    Pythion(6, 29),
    Scyllion(7, 30),
    Kyrion(8, 29),
    Illpharion(9, 30),
    Vallion(10, 29),
    Luthion(11, 30),
    Gamelion(12, 29),
    Charibdion(13, 30);

    companion object {
        fun monthFromInt(m: Int): Month
        {
            return values().first {
                it.order == m
            }
        }
    }

    fun sumOfPrecedingMonths(): Int = values().filter { it.order < this.order }.sumBy{ it.days }
    fun nextMonth(): Month = values().first { it.order == this.order + 1 }
    fun prevMonth(): Month = values().first { it.order == this.order - 1 }
}