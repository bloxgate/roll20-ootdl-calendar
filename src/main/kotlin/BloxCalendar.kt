import kotlin.js.Json
import kotlin.js.json

external fun on(event: String, func: Function<Unit>)
external fun log(message: Any)
external fun playerIsGM(playerid: String): Boolean

//Chat Functions
external fun sendChat(speakingAs: String, input: String, callback: Function<Unit>? = definedExternally, options: Json? = definedExternally)

fun main(args: Array<String>) {
    on("ready", {
        log("Bloxgate's Odyssey of the Dragonlords Calendar v${BloxCalendar.VERSION} starting up!")
        if(!state.hasOwnProperty("BloxCalendarState") || state.BloxCalendarState["version"].toString() != BloxCalendar.VERSION)
        {
            state.BloxCalendarState = json(
                "version" to BloxCalendar.VERSION,
                "year" to BloxCalendar.DEFAULT_YEAR,
                "month" to BloxCalendar.DEFAULT_MONTH,
                "day" to BloxCalendar.DEFAULT_DAY,
                "yearLen" to BloxCalendar.YEAR_LENGTH
            )
            log("Initialized default calendar state!")
        }
        BloxCalendar.initCalendar(state.BloxCalendarState["year"] as Int, state.BloxCalendarState["month"] as Int,
            state.BloxCalendarState["day"] as Int, state.BloxCalendarState["yearLen"] as Int)
        log("Calendar date populated")
    })

    on("chat:message", {msg: Json ->
        if(msg["type"].toString() == "api" && msg["content"].toString().indexOf("!cal") != -1)
        {
            //sendChat("Calendar", "Test Message", null, null)
            BloxCalendar.handleChat(msg["content"].toString().split(' '), msg["who"].toString(), msg["playerid"].toString())
        }
    })
}

object BloxCalendar {

    val DEFAULT_YEAR = 500
    val DEFAULT_MONTH = Month.monthFromInt(1).order //One-indexed
    val DEFAULT_DAY = 1 //Also one-indexed.
    var YEAR_LENGTH = 384

    const val VERSION = "a1.5"

    var curMonth = Month.monthFromInt(DEFAULT_MONTH)
    var curDay = DEFAULT_DAY
    var curYear = DEFAULT_YEAR

    fun initCalendar(year: Int, month: Int, day: Int, yLen: Int)
    {
        curYear = year
        curMonth = Month.monthFromInt(month)
        curDay = day
        YEAR_LENGTH = yLen
    }

    private fun computeDayOfWeek(d: Int): Int
    {
        var dayOfWeek = (d+6).rem(7)
        if (dayOfWeek == 0)
        {
            dayOfWeek = 7
        }
        return dayOfWeek
    }

    private fun computeWeekdayName(d: Int): String = Days.getByDayOfWeek(computeDayOfWeek(d)).name

    private fun computeDayOfMonth(d: Int) = d - curMonth.sumOfPrecedingMonths()

    private fun saveCalendarToCampaignState()
    {
        state.BloxCalendarState["year"] = curYear
        state.BloxCalendarState["month"] = curMonth.order
        state.BloxCalendarState["day"] = curDay
    }

    private fun advanceCalendar(d: Int) {
        var dayOfMonth = computeDayOfMonth(curDay)
        var daysToAdvance = d

        //log("Year advancement threshold is >=${YEAR_LENGTH - curDay + 1}. We are advancing $d")
        while(daysToAdvance >= YEAR_LENGTH - curDay + 1)
        {
            curYear++
            daysToAdvance -= (YEAR_LENGTH - curDay + 1)
            curMonth = Month.monthFromInt(1)
            curDay = 1
        }
        while(daysToAdvance > (curMonth.days - dayOfMonth))
        {
            daysToAdvance -= (curMonth.days - dayOfMonth + 1)
            curDay += (curMonth.days - dayOfMonth + 1)
            curMonth = curMonth.nextMonth()
            dayOfMonth = 1
        }
        if(daysToAdvance > 0)
        {
            curDay += daysToAdvance
        }

        saveCalendarToCampaignState()
    }

    private fun reduceCalendar(d: Int)
    {
        var dayOfMonth = computeDayOfMonth(curDay)
        var daysToReduce = d

        while(daysToReduce >= curDay)
        {
            curYear--
            daysToReduce -= curDay
            curMonth = Month.monthFromInt(13)
            curDay = 384
            dayOfMonth = computeDayOfMonth(curDay)
        }
        while(daysToReduce > dayOfMonth)
        {
            curMonth = curMonth.prevMonth()
            daysToReduce -= dayOfMonth
            curDay -= dayOfMonth
            dayOfMonth = computeDayOfMonth(curDay)
        }
        if(daysToReduce > 0)
        {
            curDay -= daysToReduce
        }

        saveCalendarToCampaignState()
    }

    private fun printMonth(m: Month)
    {
        val msg = StringBuilder("<h5>${m.name} $curYear CE</h5><table style='border: 1px solid black'><thead><tr>")
        Days.values().forEach { msg.append("<th style='border: 1px solid black'>${it.name.substring(0, 2)}</th>") }
        msg.append("</tr></thead><tbody><tr>")
        for(i in 1 .. m.days)
        {
            val day = computeDayOfWeek(m.sumOfPrecedingMonths() + i)
            if(i == 1)
            {
                for(j in 1 until day)
                {
                    msg.append("<td style='border: 1px solid black'></td>")
                }
            }
            if(curDay - m.sumOfPrecedingMonths() > i)
            {
                msg.append("<td style='border: 1px solid black'><strike>$i</strike></td>")
            } else {
                msg.append("<td style='border: 1px solid black'>$i</td>")
            }
            if(day.rem(7) == 0 && i != m.days)
            {
                msg.append("</tr><tr>")
            }
        }
        msg.append("</tr></tbody></table>")

        sendChat("Calendar Bot", msg.toString(), null, json("noarchive" to true))
    }

    fun handleChat(content: List<String>, author: String, playerid: String)
    {
        if(content.size == 1)
        {
            sendChat("Calendar Bot", "It is now ${computeWeekdayName(curDay)}, ${computeDayOfMonth(curDay)} ${curMonth.name}, $curYear CE",
                null, json("noarchive" to true))
        } else {
            val isGM = playerIsGM(playerid)
            when(content[1])
            {
                "advance" -> {
                    if(isGM)
                    {
                        val days = content.getOrNull(2)?.toInt() ?: 0
                        advanceCalendar(days)
                        sendChat("Calendar Bot", "/w gm Calendar advanced $days days")
                    } else {
                        sendChat("Calendar Bot", "/w $author You must be the GM to do that!", null, json("noarchive" to true))
                    }
                }
                "reduce" -> {
                    if(isGM)
                    {
                        val days = content.getOrNull(2)?.toInt() ?: 0
                        reduceCalendar(days)
                        sendChat("Calendar Bot", "/w gm Calendar reduced $days days")
                    } else {
                        sendChat("Calenar Bot", "/w $author You must be the GM to do that!", null, json("noarchive" to true))
                    }
                }
                "month" -> {
                    printMonth(curMonth)
                }
                "year" -> {
                    Month.values().forEach { printMonth(it) }
                }
            }
        }
    }

}