package fr.sommelle

import android.app.backup.BackupAgentHelper
import android.app.backup.FullBackupDataOutput
import android.os.Build
import fr.sommelle.core.data.backup.BackupPolicy
import fr.sommelle.core.data.backup.CloudBackupConsent

/**
 * Filtre les sauvegardes automatiques d'Android selon [BackupPolicy] : rien ne part vers le compte Google
 * sans le choix de la personne, et jamais sans chiffrement de bout en bout.
 */
class SommelleBackupAgent : BackupAgentHelper() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) data.transportFlags else 0
        val consent = CloudBackupConsent(noBackupFilesDir)
        if (BackupPolicy.allows(Build.VERSION.SDK_INT, flags, consent.enabled)) {
            super.onFullBackup(data)
        }
    }
}
