package com.getakyra.app.features.docupro

import android.content.Context
import com.getakyra.app.data.Associate
import com.getakyra.app.data.IncidentLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementGenerator {

    /** Reads the template from assets once. Call this during ViewModel init. */
    fun loadTemplate(context: Context): String {
        return try {
            context.assets.open("statement_template.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Pure function — no Android dependencies. Injects entity data into the {PLACEHOLDER}
     * template loaded by [loadTemplate].
     */
    fun generate(
        template: String,
        associate: Associate,
        incident: IncidentLog,
        managerName: String = "Shift Supervisor"
    ): String {
        if (template.isEmpty()) return "Error: Could not load statement template."

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(incident.timestampMs))

        return template
            .replace("{EMP_NAME}", associate.name)
            .replace("{EMP_ID}", associate.id.toString())
            .replace("{STORE_NUM}", "N/A")
            .replace("{DATE}", dateString)
            .replace("{WHAT_HAPPENED}", incident.description)
            .replace("{WHEN_OCCURRED}", "${incident.category} — $dateString")
            .replace("{WHERE_OCCURRED}", "[See incident description]")
            .replace("{WITNESSES}", "[Not provided]")
            .replace("{TOLD_ANYONE}", "[Not provided]")
            .replace("{ADDITIONAL_COMMENTS}", "Category: ${incident.category}\nSigned by Manager: $managerName")
    }
}
