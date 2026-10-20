import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Flyway 8.x ChecksumCalculator 等价实现：
 * readLine 逐行、仅首行过滤 BOM、每行 UTF-8 bytes 喂 CRC32、取 (int) 值。
 * 用法：java CalcChecksum <file1> [file2] ...
 */
public class CalcChecksum {

    public static void main(String[] args) throws Exception {
        for (String path : args) {
            File file = new File(path);
            CRC32 crc32 = new CRC32();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 4096);
            String line = reader.readLine();
            if (line != null) {
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                do {
                    crc32.update(line.getBytes(StandardCharsets.UTF_8));
                } while ((line = reader.readLine()) != null);
            }
            reader.close();
            System.out.println(file.getName() + " => " + (int) crc32.getValue());
        }
    }
}
