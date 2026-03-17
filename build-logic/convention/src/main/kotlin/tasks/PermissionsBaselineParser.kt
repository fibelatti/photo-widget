package tasks

import java.io.File
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.SortedSet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Comment
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import tasks.PermissionsBaselineParser.parseFile

/**
 * Helper class to the permission validation tasks.
 *
 * @see [GenerateManifestBaselineTask]
 * @see [CheckManifestPermissionsTask]
 */
internal object PermissionsBaselineParser {

    private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    private const val CUSTOM_NAMESPACE = "custom"

    private val comparator: Comparator<ManifestPermission> = compareByDescending<ManifestPermission> { it.tag }
        .thenBy { it.name }

    /**
     * Parses supported files and returns a sorted set of [ManifestPermission], which the caller can analyze.
     *
     * Supported files are:
     * - An `AndroidManifest.xml` file, usually the merged manifest to be packaged on a binary
     * - A baseline file generated with [GenerateManifestBaselineTask]
     */
    fun parseFile(file: File): SortedSet<ManifestPermission> {
        require(file.exists()) {
            "File not found at path: ${file.absolutePath}"
        }

        val document: Document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)

        val root: Element = document.documentElement

        val output: MutableSet<ManifestPermission> = mutableSetOf()

        val usesPermissionNodes: NodeList = root.getElementsByTagName(ManifestTag.USES_PERMISSION.manifestValue)
        for (i in 0 until usesPermissionNodes.length) {
            val element: Element = usesPermissionNodes.item(i) as Element
            val name: String = element.getAttributeNS(ANDROID_NAMESPACE, "name")

            if (name.isNotBlank()) {
                val comment: String = element.getAttributeNS(CUSTOM_NAMESPACE, "comment")

                output.add(
                    ManifestPermission(
                        tag = ManifestTag.entries.first { it.manifestValue == element.tagName },
                        name = name,
                        comment = comment.ifBlank { null },
                    ),
                )
            }
        }

        val permissionNodes: NodeList = root.getElementsByTagName(ManifestTag.PERMISSION.manifestValue)
        for (i in 0 until permissionNodes.length) {
            val element: Element = permissionNodes.item(i) as Element
            val name: String = element.getAttributeNS(ANDROID_NAMESPACE, "name")

            if (name.isNotBlank()) {
                val protectionLevel: String = element.getAttributeNS(ANDROID_NAMESPACE, "protectionLevel")
                val comment: String = element.getAttributeNS(CUSTOM_NAMESPACE, "comment")

                output.add(
                    ManifestPermission(
                        tag = ManifestTag.entries.first { it.manifestValue == element.tagName },
                        name = name,
                        protectionLevel = protectionLevel.ifBlank { null },
                        comment = comment.ifBlank { null },
                    ),
                )
            }
        }

        return output.toSortedSet(comparator)
    }

    /**
     * Takes [permissions] generated with [parseFile] and writes them to the given [outputFile].
     *
     * This output file should be tracked in source controls in order to be analyzed iteratively with
     * [CheckManifestPermissionsTask].
     */
    fun generateBaseline(permissions: Collection<ManifestPermission>, outputFile: File) {
        val document: Document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .newDocument()

        val root: Element = document.createElement("manifest").apply {
            setAttribute("xmlns:android", ANDROID_NAMESPACE)
            setAttribute("xmlns:documentation", CUSTOM_NAMESPACE)
        }

        document.appendChild(root)

        val header: Comment = document.createComment(
            buildString {
                appendLine()
                appendLine("Auto-generated baseline file with the expected permissions for this app.")
                appendLine("Last updated: ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)}")
            },
        )
        document.insertBefore(header, root)
        document.insertBefore(document.createComment("@formatter:off"), root)

        permissions
            .sortedWith(comparator)
            .forEach { permission ->
                val element = document.createElement(permission.tag.manifestValue).apply {
                    setAttributeNS(ANDROID_NAMESPACE, "android:name", permission.name)

                    if (!permission.protectionLevel.isNullOrBlank()) {
                        setAttributeNS(ANDROID_NAMESPACE, "android:protectionLevel", permission.protectionLevel)
                    }

                    setAttributeNS("custom", "documentation:comment", permission.comment ?: "TODO: Document reasoning")
                }

                root.appendChild(element)
            }

        val outputXmlStringWriter = StringWriter()

        TransformerFactory.newInstance()
            .newTransformer()
            .apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty(OutputKeys.ENCODING, "utf-8")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            }
            .transform(DOMSource(document), StreamResult(outputXmlStringWriter))

        val outputXmlString = outputXmlStringWriter.toString()
            .replace("-->", "-->\n")

        outputFile.writeText(outputXmlString)
    }
}

/**
 * Represents [tags][ManifestTag] declared in an `AndroidManifest.xml` file.
 *
 * @property tag the corresponding [ManifestTag]
 * @property name the qualified name of the permission
 * @property protectionLevel the `protectionLevel` of the permission, if applicable
 * @property comment the reasoning behind the usage of the permission
 */
data class ManifestPermission(
    val tag: ManifestTag,
    val name: String,
    val protectionLevel: String? = null,
    val comment: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ManifestPermission) return false

        return tag == other.tag &&
            name == other.name &&
            protectionLevel == other.protectionLevel
    }

    override fun hashCode(): Int = Objects.hash(tag, name, protectionLevel)
}

enum class ManifestTag(val manifestValue: String) {
    PERMISSION(manifestValue = "permission"),
    USES_PERMISSION(manifestValue = "uses-permission"),
}

fun ManifestPermission.toLogString(): String = "$tag: $name"
