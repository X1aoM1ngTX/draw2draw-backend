package com.xm.draw2drawbackend.utils;

/**
 * 颜色转换工具类
 */
public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    public static void main(String[] args) {
        // 测试3位HEX
        System.out.println("3位HEX测试:");
        System.out.println("0xfff -> " + getStandardColor("0xfff"));
        System.out.println("0x000 -> " + getStandardColor("0x000"));
        System.out.println("0xabc -> " + getStandardColor("0xabc"));

        // 测试4位HEX
        System.out.println("\n4位HEX测试:");
        System.out.println("0x0a00 -> " + getStandardColor("0x0a00"));
        System.out.println("0xa100 -> " + getStandardColor("0xa100"));
        System.out.println("0x0c00 -> " + getStandardColor("0x0c00"));
        System.out.println("0x1234 -> " + getStandardColor("0x1234"));

        // 测试5位HEX
        System.out.println("\n5位HEX测试:");
        System.out.println("0x020e0 -> " + getStandardColor("0x020e0"));
        System.out.println("0xe0020 -> " + getStandardColor("0xe0020"));
        System.out.println("0x0ab00 -> " + getStandardColor("0x0ab00"));
        System.out.println("0x00ab0 -> " + getStandardColor("0x00ab0"));
        System.out.println("0xa0b40 -> " + getStandardColor("0xa0b40"));

        // 测试6位HEX
        System.out.println("\n6位HEX测试:");
        System.out.println("0x020e00 -> " + getStandardColor("0x020e00"));
        System.out.println("0x00ab00 -> " + getStandardColor("0x00ab00"));
        System.out.println("0x123456 -> " + getStandardColor("0x123456"));
        System.out.println("0x000000 -> " + getStandardColor("0x000000"));
    }

    /**
     * @param rawColor 原始颜色值，必须以0x开头
     * @return 标准的6位HEX颜色值
     */
    public static String getStandardColor(String rawColor) {
        if (rawColor == null || !rawColor.startsWith("0x")) {
            throw new IllegalArgumentException("必须以0x开头的十六进制字符串");
        }
        String hex = rawColor.substring(2).toLowerCase();
        int length = hex.length();

        // 将rgb分为三块
        String r = "00", g = "00", b = "00";

        if (length == 6) {
            return "0x" + hex;
        }
        if (length == 5) {
            r = hex.substring(0, 2);
            // 如果r以0开头=> 肯定省略了1位，r则只占了一位且r=00，需要对剩下4位进行分析
            // r不已0开头，那么就没有进行省略，r就占了前两位，继续对g和b分析，需要对剩下3位进行分析
            if (r.startsWith("0")) {
                r = "00";
                // 现在剩余后面4位，不需要分析了，g和b各占2位
                g = hex.substring(1, 3);
                b = hex.substring(3, 5);
            } else {
                // 现在剩余后三位
                // 首先对g分析，如果g以0开头，那么g肯定为00，占了1位，b就是最后两位
                // 如果g不以0开头，那结果出了：b就是省略的那一位，即b= 0 + 最后一位
                g = hex.substring(2, 4);
                if (g.startsWith("0")) {
                    g = "00";
                    b = hex.substring(3, 5);
                } else {
                    b = "0" + hex.substring(length - 1);
                }
            }
        }

        if (length == 4) {
            r = hex.substring(0, 2);
            // 如果r以0开头=> r只占了1位，且r = 00，继续分析剩下的三位
            // r不已0开头，那么就没有进行省略，所以r占了前两位，剩余两位，直接在g和b左边补0即可
            if (r.startsWith("0")) {
                r = "00";
                // 现在剩余后面3位，先对g分析
                // g以0开头，那么g就占了一位，剩下的两位就是b了，所以g=00，b=最后两位
                // g不以0开头，则g占两位，最后一位就是b，b= "0" + 最后一位
                g = hex.substring(1, 3);
                if (g.startsWith("0")) {
                    g = "00";
                    b = hex.substring(2, 4);
                } else {
                    b = "0" + hex.substring(length - 1);
                }
            } else {
                // 现在剩余后2位，无需分析，b省略了1个0，g也省略了1个0，补全即可
                g = "0" + hex.substring(2, 3);
                b = "0" + hex.substring(length - 1);
            }
        }

        if (length == 3) {
            // 这种情况三位都省略了，那么直接补0即可
            // 注意：COS中 #fff 为 #0f0f0f，不是#ffffff（这是css的简写）
            r = "0" + hex.charAt(0);
            g = "0" + hex.charAt(1);
            b = "0" + hex.charAt(2);
        }

        return "0x" + r + g + b;
    }
}
