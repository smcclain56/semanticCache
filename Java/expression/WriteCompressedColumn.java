package bcf.compression;
import java.io.IOException;

import bcf.common.ActiveBitCollection;


public interface WriteCompressedColumn {
	public void writeColumn(ActiveBitCollection col)throws IOException;
}
