package com.ggstudios.tools.datafixer

import java.util.*

class Console(rootMenu: Menu) {

    companion object {

        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_BLACK = "\u001B[30m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_YELLOW = "\u001B[33m"
        const val ANSI_BLUE = "\u001B[34m"
        const val ANSI_PURPLE = "\u001B[35m"
        const val ANSI_CYAN = "\u001B[36m"
        const val ANSI_WHITE = "\u001B[37m"

        fun newBuilder(): Builder {
            return Builder()
        }
    }

    private val currentMenu: Menu = rootMenu

    private var showMenu: Boolean = false

    class ConsoleException(message: String) : Exception(message)

    class Builder {
        private val currentMenu = SimpleMenu()
        private val rootMenu = currentMenu

        fun addOption(shortDesc: String, flag: String? = null, onOptionSelected: (console: Console) -> Unit) {
            currentMenu.addMenuItem(SimpleMenuItem(shortDesc, onOptionSelected, flag))
        }

        fun setOnBlankInputHandler(cb: (console: Console) -> Unit) {
            currentMenu.onBlankInputHandler = cb
        }

        fun build(): Console {
            return Console(rootMenu)
        }
    }

    private class SimpleMenu : Menu {

        private var _menuItems: MutableList<MenuItem> = ArrayList()

        var onBlankInputHandler: ((console: Console) -> Unit)? = null

        override val menuItems: List<MenuItem>
            get() = _menuItems

        override fun addMenuItem(menuItem: MenuItem) {
            _menuItems.add(menuItem)
        }

        @Throws(Exception::class)
        override fun onInput(console: Console, input: String) {
            val inputAsInt = input.toIntOrNull()
            if (inputAsInt != null) {
                val index = inputAsInt - 1
                if (index < 0 || index >= menuItems.size) {
                    throw ConsoleException("Option is out of bounds.")
                }
                val menuItem = menuItems[Integer.valueOf(input) - 1]

                if (menuItem is SimpleMenuItem) {
                    menuItem.onOptionSelected(console)
                } else {
                    throw RuntimeException()
                }
            } else if (input.isBlank()) {
                onBlankInputHandler?.invoke(console)
            } else {
                for (menuItem in menuItems) {
                    if (menuItem is SimpleMenuItem) {
                        if (menuItem.flag?.split(",")?.map { it.trim() }?.contains(input) == true) {
                            menuItem.onOptionSelected(console)
                        }
                    }
                }
            }
        }
    }

    private class SimpleMenuItem(
            val shortDesc: String,
            val onOptionSelected: (console: Console) -> Unit,
            /**
             * Flag that may be used as a shortcut for this option.
             */
            val flag: String? = null
    ) : MenuItem

    interface Menu : MenuItem {
        val menuItems: List<MenuItem>
        fun addMenuItem(menuItem: MenuItem)
        @Throws(Exception::class)
        fun onInput(console: Console, input: String)
    }

    interface MenuItem

    fun show() {
        showMenu = true
        while (showMenu) {
            printMenu(currentMenu)

            try {
                currentMenu.onInput(this, readNextLine())
            } catch (e: ConsoleException) {
                printError(e.message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun readNextLine(): String {
        return readLine()!!
    }

    private fun printError(message: String?) {
        println("")
        println("------------------------------")
        println(message)
        println("------------------------------")
        println("")
    }

    fun exitMenu() {
        showMenu = false
    }

    private fun printMenu(rootMenu: Menu) {
        val menuItems = rootMenu.menuItems

        for ((i, menuItem) in menuItems.withIndex()) {
            printMenuItem(i + 1, menuItem)
        }
    }

    private fun printMenuItem(optionNumber: Int, menuItem: MenuItem) {
        if (menuItem is SimpleMenuItem) {
            println("$optionNumber) ${menuItem.shortDesc}" + if (menuItem.flag != null) {
                " (${menuItem.flag})"
            } else {
                ""
            })
        } else {
            throw RuntimeException()
        }
    }
}
