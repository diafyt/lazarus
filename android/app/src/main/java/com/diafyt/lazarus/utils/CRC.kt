package com.diafyt.lazarus.utils

/**
 * Checksum which is a modified version of CRC-16/MCRF4XX [1].
 *
 * The modification is that the bits of the checksum are reversed as the very
 * last step before the checksum is returned to the user.
 *
 * Note that CRC-16/MCRF4XX is very similar to CRC-16/CCITT [2] and
 * differs only in the initial value.
 *
 * [1] CRC Algorithm for MCRF45X Read/Write Device
 *     https://ww1.microchip.com/downloads/en/AppNotes/00752a.pdf
 * [2] ITU-T Recommendation V.41
 *     https://www.itu.int/rec/T-REC-V.41/en
 */
class CRC {
    private var crc: Short = 0xFFFF.toShort()

    /**
     * Lookup table making the computation of the checksum more efficient.
     */
    private val lookupTable: ShortArray = shortArrayOf(
        0x0000.toShort(), 0x1189.toShort(), 0x2312.toShort(), 0x329b.toShort(),
        0x4624.toShort(), 0x57ad.toShort(), 0x6536.toShort(), 0x74bf.toShort(),
        0x8c48.toShort(), 0x9dc1.toShort(), 0xaf5a.toShort(), 0xbed3.toShort(),
        0xca6c.toShort(), 0xdbe5.toShort(), 0xe97e.toShort(), 0xf8f7.toShort(),
        0x1081.toShort(), 0x0108.toShort(), 0x3393.toShort(), 0x221a.toShort(),
        0x56a5.toShort(), 0x472c.toShort(), 0x75b7.toShort(), 0x643e.toShort(),
        0x9cc9.toShort(), 0x8d40.toShort(), 0xbfdb.toShort(), 0xae52.toShort(),
        0xdaed.toShort(), 0xcb64.toShort(), 0xf9ff.toShort(), 0xe876.toShort(),
        0x2102.toShort(), 0x308b.toShort(), 0x0210.toShort(), 0x1399.toShort(),
        0x6726.toShort(), 0x76af.toShort(), 0x4434.toShort(), 0x55bd.toShort(),
        0xad4a.toShort(), 0xbcc3.toShort(), 0x8e58.toShort(), 0x9fd1.toShort(),
        0xeb6e.toShort(), 0xfae7.toShort(), 0xc87c.toShort(), 0xd9f5.toShort(),
        0x3183.toShort(), 0x200a.toShort(), 0x1291.toShort(), 0x0318.toShort(),
        0x77a7.toShort(), 0x662e.toShort(), 0x54b5.toShort(), 0x453c.toShort(),
        0xbdcb.toShort(), 0xac42.toShort(), 0x9ed9.toShort(), 0x8f50.toShort(),
        0xfbef.toShort(), 0xea66.toShort(), 0xd8fd.toShort(), 0xc974.toShort(),
        0x4204.toShort(), 0x538d.toShort(), 0x6116.toShort(), 0x709f.toShort(),
        0x0420.toShort(), 0x15a9.toShort(), 0x2732.toShort(), 0x36bb.toShort(),
        0xce4c.toShort(), 0xdfc5.toShort(), 0xed5e.toShort(), 0xfcd7.toShort(),
        0x8868.toShort(), 0x99e1.toShort(), 0xab7a.toShort(), 0xbaf3.toShort(),
        0x5285.toShort(), 0x430c.toShort(), 0x7197.toShort(), 0x601e.toShort(),
        0x14a1.toShort(), 0x0528.toShort(), 0x37b3.toShort(), 0x263a.toShort(),
        0xdecd.toShort(), 0xcf44.toShort(), 0xfddf.toShort(), 0xec56.toShort(),
        0x98e9.toShort(), 0x8960.toShort(), 0xbbfb.toShort(), 0xaa72.toShort(),
        0x6306.toShort(), 0x728f.toShort(), 0x4014.toShort(), 0x519d.toShort(),
        0x2522.toShort(), 0x34ab.toShort(), 0x0630.toShort(), 0x17b9.toShort(),
        0xef4e.toShort(), 0xfec7.toShort(), 0xcc5c.toShort(), 0xddd5.toShort(),
        0xa96a.toShort(), 0xb8e3.toShort(), 0x8a78.toShort(), 0x9bf1.toShort(),
        0x7387.toShort(), 0x620e.toShort(), 0x5095.toShort(), 0x411c.toShort(),
        0x35a3.toShort(), 0x242a.toShort(), 0x16b1.toShort(), 0x0738.toShort(),
        0xffcf.toShort(), 0xee46.toShort(), 0xdcdd.toShort(), 0xcd54.toShort(),
        0xb9eb.toShort(), 0xa862.toShort(), 0x9af9.toShort(), 0x8b70.toShort(),
        0x8408.toShort(), 0x9581.toShort(), 0xa71a.toShort(), 0xb693.toShort(),
        0xc22c.toShort(), 0xd3a5.toShort(), 0xe13e.toShort(), 0xf0b7.toShort(),
        0x0840.toShort(), 0x19c9.toShort(), 0x2b52.toShort(), 0x3adb.toShort(),
        0x4e64.toShort(), 0x5fed.toShort(), 0x6d76.toShort(), 0x7cff.toShort(),
        0x9489.toShort(), 0x8500.toShort(), 0xb79b.toShort(), 0xa612.toShort(),
        0xd2ad.toShort(), 0xc324.toShort(), 0xf1bf.toShort(), 0xe036.toShort(),
        0x18c1.toShort(), 0x0948.toShort(), 0x3bd3.toShort(), 0x2a5a.toShort(),
        0x5ee5.toShort(), 0x4f6c.toShort(), 0x7df7.toShort(), 0x6c7e.toShort(),
        0xa50a.toShort(), 0xb483.toShort(), 0x8618.toShort(), 0x9791.toShort(),
        0xe32e.toShort(), 0xf2a7.toShort(), 0xc03c.toShort(), 0xd1b5.toShort(),
        0x2942.toShort(), 0x38cb.toShort(), 0x0a50.toShort(), 0x1bd9.toShort(),
        0x6f66.toShort(), 0x7eef.toShort(), 0x4c74.toShort(), 0x5dfd.toShort(),
        0xb58b.toShort(), 0xa402.toShort(), 0x9699.toShort(), 0x8710.toShort(),
        0xf3af.toShort(), 0xe226.toShort(), 0xd0bd.toShort(), 0xc134.toShort(),
        0x39c3.toShort(), 0x284a.toShort(), 0x1ad1.toShort(), 0x0b58.toShort(),
        0x7fe7.toShort(), 0x6e6e.toShort(), 0x5cf5.toShort(), 0x4d7c.toShort(),
        0xc60c.toShort(), 0xd785.toShort(), 0xe51e.toShort(), 0xf497.toShort(),
        0x8028.toShort(), 0x91a1.toShort(), 0xa33a.toShort(), 0xb2b3.toShort(),
        0x4a44.toShort(), 0x5bcd.toShort(), 0x6956.toShort(), 0x78df.toShort(),
        0x0c60.toShort(), 0x1de9.toShort(), 0x2f72.toShort(), 0x3efb.toShort(),
        0xd68d.toShort(), 0xc704.toShort(), 0xf59f.toShort(), 0xe416.toShort(),
        0x90a9.toShort(), 0x8120.toShort(), 0xb3bb.toShort(), 0xa232.toShort(),
        0x5ac5.toShort(), 0x4b4c.toShort(), 0x79d7.toShort(), 0x685e.toShort(),
        0x1ce1.toShort(), 0x0d68.toShort(), 0x3ff3.toShort(), 0x2e7a.toShort(),
        0xe70e.toShort(), 0xf687.toShort(), 0xc41c.toShort(), 0xd595.toShort(),
        0xa12a.toShort(), 0xb0a3.toShort(), 0x8238.toShort(), 0x93b1.toShort(),
        0x6b46.toShort(), 0x7acf.toShort(), 0x4854.toShort(), 0x59dd.toShort(),
        0x2d62.toShort(), 0x3ceb.toShort(), 0x0e70.toShort(), 0x1ff9.toShort(),
        0xf78f.toShort(), 0xe606.toShort(), 0xd49d.toShort(), 0xc514.toShort(),
        0xb1ab.toShort(), 0xa022.toShort(), 0x92b9.toShort(), 0x8330.toShort(),
        0x7bc7.toShort(), 0x6a4e.toShort(), 0x58d5.toShort(), 0x495c.toShort(),
        0x3de3.toShort(), 0x2c6a.toShort(), 0x1ef1.toShort(), 0x0f78.toShort()
    )

    /**
     * Retrieve the final CRC.
     *
     * This makes the customizing and reverses the bits in the checksum.
     *
     * @return checksum
     */
    fun getCRC(): Short {
        // make all calculations in a wider data type to work around the deficits of Java
        // which does not have unsigned data types causing lots of trouble
        var ret = 0
        var temp = crc.toInt() and 0xFFFF
        for(i in 0 until 0x10) {
            ret = (ret shl 1) or (temp and 0x1)
            temp = temp shr 1
        }
        return ret.toShort()
    }

    /**
     * Add data to be included in the checksum.
     *
     * This does the real work by performing the CRC-16/MCRF4XX routine.
     *
     * @param b input data
     */
    fun update(b: Byte) {
        // make all calculations in a wider data type to work around the deficits of Java
        // which does not have unsigned data types causing lots of trouble
        val crcU = crc.toInt() and 0xFFFF
        val bU = b.toInt() and 0xFF

        crc = (lookupTable[(bU xor crcU) and 0xFF].toInt() xor (crcU shr 8)).toShort()
    }
}
