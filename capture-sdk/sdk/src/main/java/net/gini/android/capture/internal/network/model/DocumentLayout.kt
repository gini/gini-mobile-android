package net.gini.android.capture.internal.network.model

data class DocumentLayout(
    val pages: List<Page>
) {

    data class Page(
        val number: Int,
        val sizeX: Float,
        val sizeY: Float,
        val textZones: List<TextZone>,
        val regions: List<Region>
    ) {

        data class TextZone(
            val paragraphs: List<Paragraph>,
        ) {

            data class Paragraph(
                val width: Float,
                val height: Float,
                val top: Float,
                val left: Float,
                val lines: List<Line>,
            ) {

                data class Line(
                    val width: Float,
                    val height: Float,
                    val top: Float,
                    val left: Float,
                    val words: List<Word>,
                ) {

                    data class Word(
                        val width: Float,
                        val height: Float,
                        val top: Float,
                        val left: Float,
                        val fontSize: Float,
                        val fontFamily: String,
                        val bold: Boolean,
                        val text: String,
                    )
                }
            }
        }

        data class Region(
            val width: Float,
            val height: Float,
            val top: Float,
            val left: Float,
            val type: String,
        )
    }
}
