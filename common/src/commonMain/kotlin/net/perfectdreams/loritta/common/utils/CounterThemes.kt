package net.perfectdreams.loritta.common.utils

enum class CounterThemes(val localizedName: String, val emotes: List<String>?) {
    DEFAULT(
        "loritta.modules.counter.theme.default",
        listOf(
            "<:twitter_zero:477236755519504385>",
            "1⃣",
            "2⃣",
            "3⃣",
            "4⃣",
            "5⃣",
            "6⃣",
            "7⃣",
            "8⃣",
            "9⃣"
        )
    ),
    RED(
        "loritta.modules.counter.theme.red",
        listOf(
            "<:red_zero:517072286294081539>",
            "<:red_one:517072286293950464>",
            "<:red_two:517072286206001152>",
            "<:red_three:517072286445207610>",
            "<:red_four:517072286164058112>",
            "<:red_five:517072286184898560>",
            "<:red_six:517072286264721408>",
            "<:red_seven:517072286617174043>",
            "<:red_eight:517072285841096727>",
            "<:red_nine:517072286138761228>"
        )
    ),
    GREEN(
        "loritta.modules.counter.theme.green",
        listOf(
            "<:green_zero:517073192708341762>",
            "<:green_one:517073193018720256>",
            "<:green_two:517073192687239190>",
            "<:green_three:517073193064726528>",
            "<:green_four:517073194088267776>",
            "<:green_five:517073192892891153>",
            "<:green_six:517073192976908338>",
            "<:green_seven:517073192964325377>",
            "<:green_eight:517073193043886090>",
            "<:green_nine:517073193043886080>"
        )
    ),
    BLURPLE(
        "loritta.modules.counter.theme.blurple",
        listOf(
            "<:blurple_zero:517085610436198411>",
            "<:blurple_one:517085610792583169>",
            "<:blurple_two:517085610750771200>",
            "<:blurple_three:517085610754703360>",
            "<:blurple_four:517085610687856640>",
            "<:blurple_five:517085610595319829>",
            "<:blurple_six:517085610800971797>",
            "<:blurple_seven:517085611061149716>",
            "<:blurple_eight:517085610956029964>",
            "<:blurple_nine:517085610691919872>"
        )
    ),
    BLACK(
        "loritta.modules.counter.theme.black",
        listOf(
            "<:black_zero:517089812218380289>",
            "<:black_one:517089812172374036>",
            "<:black_two:517089812101201930>",
            "<:black_three:517089812264517642>",
            "<:black_four:517089812012859393>",
            "<:black_five:517089811690029064>",
            "<:black_six:517089812163854336>",
            "<:black_seven:517089811996213269>",
            "<:black_eight:517089811908264097>",
            "<:black_nine:517089811916652616>"
        )
    ),
    DELUXE(
        "loritta.modules.counter.theme.deluxe",
        listOf(
            "<a:deluxe_zero:528323607433183242>",
            "<a:deluxe_one:528323712055902210>",
            "<a:deluxe_two:528323760881795096>",
            "<a:deluxe_three:528323825864015873>",
            "<a:deluxe_four:528323887033876491>",
            "<a:deluxe_five:528323921397940245>",
            "<a:deluxe_six:528323973428019200>",
            "<a:deluxe_seven:528324015291367446>",
            "<a:deluxe_eight:528324053732294687>",
            "<a:deluxe_nine:528324101518000138>"
        )
    ),
    LORITTA(
        "loritta.modules.counter.theme.loritta",
        listOf(
            "<:lori_zero:538508557067223041>",
            "<:lori_one:538507099102248983>",
            "<:lori_two:538507158044672001>",
            "<:lori_three:538507214709719061>",
            "<:lori_four:538507252575764490>",
            "<:lori_five:538507300122394634>",
            "<:lori_six:538507324936159254>",
            "<:lori_seven:538507359631310869>",
            "<:lori_eight:538507403658919966>",
            "<:lori_nine:538507458914811934>"
        )
    ),
    LORITTA_KAWAI(
        "loritta.modules.counter.theme.loritta-kawaii",
        listOf(
            "<a:kawaii_zero:542823087649849414>",
            "<:kawaii_one:542823112220344350>",
            "<a:kawaii_two:542823168465829907>",
            "<a:kawaii_three:542823194445348885>",
            "<:kawaii_four:542823233448050688>",
            "<a:kawaii_five:542823247826386997>",
            "<:kawaii_six:542823279858286592>",
            "<a:kawaii_seven:542823307414601734>",
            "<:kawaii_eight:542823334652411936>",
            "<:kawaii_nine:542823384917213200>"
        )
    ),
    // This is unused
    /* CUSTOM(
        "loritta.modules.counter.theme.custom",
        null
    ) */
}