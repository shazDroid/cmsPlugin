import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import javax.swing.*
import kotlin.properties.Delegates

class InputContentDialog : DialogWrapper(true) {
    private lateinit var cmsKeyMapperInput: JTextField
    private lateinit var englishContentInput: JTextArea
    private lateinit var arabicContentInput: JTextArea

    val fileModifier = FileModifier()
    val fileService = service<FileSelectionService>()
    val jsonFileModifier = JsonFileModifier()

    init {
        title = "Add Cms String"
        init()  // Call to set up the dialog
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        cmsKeyMapperInput = JTextField(20)
        englishContentInput = JTextArea(5, 20)
        arabicContentInput = JTextArea(5, 20)

        panel.add(JLabel("Cms Key: "))
        panel.add(cmsKeyMapperInput)
        panel.add(JLabel("English Content: "))
        panel.add(englishContentInput)
        panel.add(JLabel("Arabic Content: "))
        panel.add(arabicContentInput)

        return panel
    }

    override fun doOKAction() {
        val cmsKey = cmsKeyMapperInput.text
        val engContent = englishContentInput.text
        val arContent = arabicContentInput.text

        if (cmsKey.isNotEmpty() && engContent.isNotEmpty() && arContent.isNotEmpty()) {
            fileService.getSelectedFiles().first()
            fileService.getSelectedFiles().forEachIndexed { index, item ->
                if (item.contains("CmsKeyMapper.kt")) {
                    fileModifier.appendCmsKeyToFile(item, cmsKey)
                }

                if (item.contains("DefaultEn.json")) {
                    jsonFileModifier.appendToEnglishJson(
                        item,
                        cmsKey,
                        engContent
                    )
                }

                if (item.contains("DefaultAr.json")) {
                    jsonFileModifier.appendToEnglishJson(
                        item,
                        cmsKey,
                        arContent
                    )
                }
            }

            Messages.showMessageDialog("Cms Key Added Successfully", "Success", Messages.getInformationIcon())

            super.doOKAction()  // Close the dialog
        } else {
            Messages.showMessageDialog("Please fill in all three inputs.", "Error", Messages.getErrorIcon())
        }
    }
}
