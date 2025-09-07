package ir.bamap.blu.jpa.util

import ir.bamap.blu.model.filter.FilterModel
import ir.bamap.blu.model.filter.GroupFilter
import ir.bamap.blu.model.filter.NotFilter
import java.security.InvalidParameterException
import java.util.*

class Util {
    companion object {
        const val BIG_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val SMALL_LETTERS = "abcdefghijklmnopqrstuvwxyz"
        const val SIGNS = "~!@#$%^&*()_-+"
        const val NUMBERS = "0123456789"

        @JvmStatic
        fun nextString(length: Int, characters: String = "$BIG_LETTERS$SMALL_LETTERS$NUMBERS"): String {
            return List(length) {
                characters.random()
            }.joinToString("")
        }

        fun getValueSqlClause(value: Any): String {
            if (literalIsField(value))
                return "\"${getField(value)}\""

            if (value is String)
                return "'$value'"

            return "$value"
        }

        fun getValidLiteralType(literal: String): Any {

            val boolean = toBooleanOrNull(literal)
            if (boolean != null)
                return boolean

            val number = toNumberOrNull(literal)
            if (number != null)
                return number

            return literal
        }

        fun flat(filters: MutableCollection<FilterModel>) : Collection<FilterModel> {
            filters.removeIf { removeEmptyHierarchy(it) }

            return filters
        }

        fun removeSpaceFromField(field: String): String {
            return field.replace(" ", "")
        }

        private fun getField(literal: Any): String {
            val regex = "\\{\\{.*\\}\\}".toRegex()

            val match = regex.find(literal.toString()) ?: throw RuntimeException("")
            val value = match.value
            return value.substring(2, value.length - 2)
        }

        private fun literalIsField(literal: Any): Boolean {
            if (literal !is String)
                return false

            val regex = "\\{\\{.*\\}\\}".toRegex()
            return literal.matches(regex)
        }

        private fun getBigParenthesesSegment(queryString: String): String? {
            var count = 0
            var startIndex = -1
            var endIndex = -1

            for (i in queryString.indices) {
                val char = queryString[i]
                if (char == '(') {
                    if (count == 0) {
                        startIndex = i
                    }
                    count++
                    continue
                }

                if (char == ')') {
                    count--
                    if (count == 0) {
                        endIndex = i
                        break
                    }
                }
            }

            if (startIndex > endIndex)
                throw InvalidParameterException("query string[$queryString] is wrong")

            if (startIndex == endIndex)
                return null

            return queryString.substring(startIndex, endIndex + 1)
        }

        private fun toNumberOrNull(value: Any?): Number? {
            if (value == null)
                return null

            if (value is Number)
                return value

            val stringValue = value.toString();
            val int = stringValue.toIntOrNull()
            if (int != null)
                return int

            val long = stringValue.toLongOrNull()
            if (long != null)
                return long

            val float = stringValue.toFloatOrNull()
            if (float != null && float.isFinite())
                return float

            return stringValue.toDoubleOrNull()
        }

        /**
         * @return get Boolean value type if [value] is similar to TRUE, True and etc.
         */
        private fun toBooleanOrNull(value: Any?): Boolean? {
            if (value == null)
                return null

            if (value is Boolean)
                return value

            return when (value.toString().lowercase(Locale.getDefault())) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }

        private fun removeEmptyHierarchy(filter: FilterModel): Boolean {
            if(filter is GroupFilter) {
                filter.filters.flatten()
                return filter.filters.isEmpty()
            }

            if(filter is NotFilter)
                return removeEmptyHierarchy(filter.filter)

            // is OperatorFilter
            return false
        }
    }
}