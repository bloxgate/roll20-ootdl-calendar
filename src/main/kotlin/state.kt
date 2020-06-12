import kotlin.js.Json

external object state {
    var hasOwnProperty: (property: String) -> Boolean
    var BloxCalendarState: Json
}