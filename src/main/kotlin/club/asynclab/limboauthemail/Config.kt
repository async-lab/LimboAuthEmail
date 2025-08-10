package club.asynclab.limboauthemail

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.io.IOException
import java.nio.file.Files


class Config(private val plugin: LimboAuthEmail) {
    private val rootNodeMap = HashMap<String, CommentedConfigurationNode>()

    fun getRootNode(fileName: String?) = this.rootNodeMap[fileName]

    @Throws(ConfigurateException::class)
    fun loadFile(fileName: String): File {
        val dataDirFile = this.plugin.dataDirectory.toFile()
        if (!dataDirFile.exists()) {
            if (!dataDirFile.mkdir()) {
                throw RuntimeException("ERROR: Can't create data directory (permissions/filesystem error?)")
            }
        }

        val dataFile = File(dataDirFile, fileName)
        if (!dataFile.exists()) {
            try {
                val bundleFile = this.javaClass.getResourceAsStream("/$fileName")
                if (bundleFile != null) {
                    val dataFileDir = dataFile.parentFile
                    if (!dataFileDir.exists()) {
                        if (!dataFileDir.mkdir()) {
                            throw RuntimeException("ERROR: Can't create data directory (permissions/filesystem error?)")
                        }
                    }
                    Files.copy(bundleFile, dataFile.toPath())
                }
            } catch (e: IOException) {
                throw RuntimeException("ERROR: Can't write default configuration file: ${e.message}")
            }
        }
        return dataFile
    }

    fun loadConfiguration(fileName: String) {
        this.rootNodeMap.put(
            fileName,
            YamlConfigurationLoader.builder().path(this.loadFile(fileName).toPath()).build().load()
        )
    }
}