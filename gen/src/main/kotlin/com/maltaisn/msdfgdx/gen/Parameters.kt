/*
 * Copyright 2019 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.msdfgdx.gen

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import java.io.File
import java.io.IOException


@Parameters(separators = " =")
class Parameters {

    @Parameter
    var params: List<String> = mutableListOf()

    @Parameter(names = ["-g", "--msdfgen"], description = "Path of the msdfgen executable", order = 0)
    var msdfgen: String = "msdfgen.exe"

    @Parameter(names = ["-o", "--output"], description = "Output path of generated font textures", order = 1)
    var output: String? = null

    @Parameter(names = ["-t", "--field-type"], description = "Field type: sdf | psdf | msdf | mtsdf", order = 2)
    var fieldType: String = FIELD_TYPE_MSDF

    @Parameter(names = ["-a", "--alpha-field-type"], description = "Alpha field type: none | sdf | psdf", order = 3)
    var alphaFieldType: String = FIELD_TYPE_SDF

    @Parameter(names = ["-s", "--font-size"], description = "Font size for generated textures", order = 4)
    var fontSize: Int = 32

    @Parameter(names = ["-r", "--distance-range"], description = "Distance range in which SDF is encoded", order = 5)
    var distanceRange: Int = 5

    @Parameter(names = ["-d", "--texture-size"], arity = 2, description = "Maximum width and height of generated atlas pages", order = 6)
    var textureSize: List<Int> = listOf(512, 512)

    @Parameter(names = ["-p", "--padding"],
        description = "Padding between glyphs and on the border of the atlas pages",
        order = 7)
    var padding: Int = 2

    @Parameter(names = ["-c", "--charset"], description = "File containing the characters to use (encoded as UTF-8). " +
            "Can also be one of: ascii, ascii-extended, latin-0, latin-9, windows-1252, extended.", order = 8)
    var charset: String = "ascii"

    @Parameter(names = ["--compression-level"],
        description = "Compression level for generated PNG, from 0 to 9",
        order = 9)
    var compressionLevel = 9

    @Parameter(names = ["--fast-pack"],
        description = "Whether to use the faster but less efficient packing algorithm",
        order = 10)
    var fastPacking: Boolean = false

    @Parameter(names = ["-h", "--help"], description = "Show help message", help = true, order = 11)
    var help = false

    @Parameter(names = ["-v", "--version"], description = "Show version", order = 12)
    var version = false

    /** List of characters from charset. */
    var charList = ""
        private set

    /** Output directory. */
    var outputDir = System.getProperty("user.dir")
        private set

    /**
     * Whether output bitmap font has glyphs encoded in alpha channel or not.
     */
    val hasAlphaChannel: Boolean
        get() = alphaFieldType != FIELD_TYPE_NONE || fieldType == FIELD_TYPE_MTSDF

    /**
     * Validate arguments
     */
    fun validate() {
        // Validate input files
        if (params.isEmpty()) paramError("No input file.")
        for (inputFile in params) {
            if (!File(inputFile).exists()) {
                paramError("Input file '$inputFile' doesn't exist.")
            }
        }

        // Create output directory
        if (output != null) outputDir = output
        val outputPath = File(outputDir)
        outputPath.mkdirs()
        if (!outputPath.exists()) paramError("Could not create output directory at '$outputDir'.")

        // Check is msdfgen executable can be executed.
        try {
            Runtime.getRuntime().exec(msdfgen)
        } catch (e: IOException) {
            paramError("msdfgen executable '$msdfgen' doesn't exist or isn't executable.")
        }

        // Due to a JCommander bug, a list parameter default value is not overwritten, only appended.
        // https://github.com/cbeust/jcommander/issues/137
        if (textureSize.size == 4) {
            textureSize = textureSize.subList(2, 4)
        }

        // Validate other arguments
        when {
            fieldType !in listOf(FIELD_TYPE_SDF, FIELD_TYPE_PSDF, FIELD_TYPE_MSDF, FIELD_TYPE_MTSDF) ->
                paramError("Invalid field type '$fieldType'")
            alphaFieldType !in listOf(FIELD_TYPE_NONE, FIELD_TYPE_SDF, FIELD_TYPE_PSDF) ->
                paramError("Invalid field type '$alphaFieldType'")
            fontSize < 8 -> paramError("Font size must be at least 8.")
            distanceRange < 1 -> paramError("Distance range must be at least 1.")
            textureSize.any { d -> d !in VALID_TEXTURE_SIZES } -> paramError("Texture size must be power of two between 32 and 65536.")
            padding < 0 -> paramError("Padding must be at least 0.")
        }

        if (alphaFieldType != FIELD_TYPE_NONE && fieldType == FIELD_TYPE_MTSDF) {
            // if using mtsdf, msdfgen will take care of generating the alpha channel, so alpha field type has no effect.
            println("WARNING: alpha field type is ignored when using mtsdf field type.")
        }

        // Get charset from file or builtin
        // Also remove duplicate characters and sort them.
        charList = BUILTIN_CHARSETS[charset] ?: try {
            File(charset).readText()
        } catch (e: IOException) {
            paramError("Could not read charset file.")
        }.toSortedSet().joinToString("")
    }

    /**
     * Return a summary of the parameter values.
     */
    fun summarize() = """
        |Font files:
        |  - ${params.joinToString("\n  - ") { File(it).absolutePath }}
        |Output: ${File(outputDir).absolutePath}
        |Field type: $fieldType
        |Alpha field type: $alphaFieldType
        |Font size: $fontSize px
        |Distance range: $distanceRange px
        |Texture size: ${textureSize[0]} x ${textureSize[1]} px
        |Padding: $padding px
        |Charset: ${charList.length} chars
        |Compression level: $compressionLevel
        |Fast packing: $fastPacking
    """.trimMargin()

    companion object {
        const val FIELD_TYPE_NONE = "none"
        const val FIELD_TYPE_SDF = "sdf"
        const val FIELD_TYPE_PSDF = "psdf"
        const val FIELD_TYPE_MSDF = "msdf"
        const val FIELD_TYPE_MTSDF = "mtsdf"

        private val VALID_TEXTURE_SIZES = List(12) { 1 shl (it + 5) }

        private val BUILTIN_CHARSETS = mapOf(
            "test" to " A@jp&ÂO!-$",
            /* ASCII printable chars up to 127. */
            "ascii" to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
            /* ASCII printable chars up to 255 minus box chars and some math. */
            "ascii-extended" to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|} ~ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤÷≈°∙·√ⁿ²■",
            /* ISO/IEC 8859-1 aka latin-0. */
            "latin-0" to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ",
            /* ISO/IEC 8859-15 aka latin-9. */
            "latin-9" to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ¡¢£€¥Š§š©ª«¬\u00AD®¯°±²³Žµ¶·ž¹º»ŒœŸ¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ",
            /* windows-1252 (superset of latin-0). */
            "windows-1252" to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿŒœŠšŸŽžƒˆ˜–—‘’‚“”„†‡•…‰‹›€™",
            /* Hiero's extended charset. */
            "extended" to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ ¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢ" +
                    "ģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſʹ͵ͺͻͼͽ;΄΅Ά·ΈΉΊΌΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿЀЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕ" +
                    "ЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюяѐёђѓєѕіїјљњћќѝўџѠѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿҀҁ҂҃҄҅҆҇҈҉ҊҋҌҍҎҏҐґҒғҔҕҖҗҘҙҚқҜҝҞҟҠҡҢңҤҥҦҧҨҩҪҫҬҭҮүҰұҲҳҴҵҶҷҸҹҺһҼҽҾҿӀӁӂӃӄӅӆӇӈӉӊӋӌӍӎӏӐӑӒӓӔӕӖӗӘәӚӛӜӝӞӟӠӡӢӣӤӥӦӧӨөӪӫӬӭӮӯӰӱӲӳӴӵӶӷӸӹӺӻӼӽӾӿԀԁԂԃԄԅԆԇԈԉԊԋԌ" +
                    "ԍԎԏԐԑԒԓԔԕԖԗԘԙԚԛԜԝԞԟԠԡԢԣԤԥԦԧ           \u200B\u200C\u200D\u200E\u200F‒–—―‖‗‘’‚‛“”„‟†‡•…\u202A\u202B\u202C\u202D\u202E ‰′″‴‹›‼‾⁄⁞\u206A\u206B\u206C\u206D\u206E\u206F₠₡₢₣₤₥₦₧₨₩₫€₭₮₯₰₱₲₳₴₵₹₺ⱠⱡⱢⱣⱤⱥⱦⱧⱨⱩⱪⱫⱬⱭⱱⱲⱳⱴⱵⱶⱷ"
        )
    }

}
