import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
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
        val panel = JPanel(GridBagLayout())
        panel.preferredSize = Dimension(400,500)
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            weightx = 1.0
            insets = Insets(5, 5, 5, 5) // Padding around components
        }

        // Cms Key Label and Input
        panel.add(JLabel("Cms Key: "), constraints.apply { gridy = 0; gridx = 0 })
        cmsKeyMapperInput = JTextField(20)
        panel.add(cmsKeyMapperInput, constraints.apply { gridy = 0; gridx = 1 })

        // English Content Label and Input
        panel.add(JLabel("English Content: "), constraints.apply { gridy = 1; gridx = 0 })
        englishContentInput = JTextArea(8, 30)
        panel.add(JScrollPane(englishContentInput), constraints.apply { gridy = 1; gridx = 1 })

        // Arabic Content Label and Input
        panel.add(JLabel("Arabic Content: "), constraints.apply { gridy = 2; gridx = 0 })
        arabicContentInput = JTextArea(8, 30)
        panel.add(JScrollPane(arabicContentInput), constraints.apply { gridy = 2; gridx = 1 })

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

                if (item.contains("DefaultArabic.json")) {
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
