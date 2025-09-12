package sk.v2.plugins.teamnotify.settings

import jetbrains.buildServer.serverSide.settings.ProjectSettings
import org.jdom.Element

class DisabledWebhooksSettings : ProjectSettings {
    val disabledWebhookUrls: MutableSet<String> = mutableSetOf()
    
    override fun readFrom(parentElement: Element) {
        disabledWebhookUrls.clear()
        
        val urlsElement = parentElement.getChild("disabledUrls") ?: return
        val urlElements = urlsElement.getChildren("url")
        
        for (urlObj in urlElements) {
            val urlElement = urlObj as? Element ?: continue
            val url = urlElement.textTrim
            if (url.isNotBlank()) {
                disabledWebhookUrls.add(url)
            }
        }
    }

    override fun writeTo(parentElement: Element) {
        val urlsElement = Element("disabledUrls")
        parentElement.addContent(urlsElement)
        
        for (url in disabledWebhookUrls) {
            urlsElement.addContent(Element("url").setText(url))
        }
    }
    
    override fun dispose() {
        disabledWebhookUrls.clear()
    }
}