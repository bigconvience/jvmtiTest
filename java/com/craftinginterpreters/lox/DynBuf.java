package com.craftinginterpreters.lox;

import java.util.Arrays;

/**
 * @author benpeng.jiang
 * @title: DynBuf
 * @projectName LoxScript
 * @description: TODO
 * @date 2021/2/251:38 PM
 */
public class DynBuf {
  byte[] buf = new byte[0];
  int size;
  int allocatedSize;
  boolean error;

  private void memcpy(byte[] data, int len) {
    for (int i = 0; i < len; i++) {
      buf[size + i] = data[i];
    }
  }

  int realloc(int newSize) {
    if (newSize > allocatedSize) {
      if (error) {
        return -1;
      }
      int targetSize = allocatedSize * 3 / 2;
      if (targetSize > newSize) {
        newSize = targetSize;
      }
      buf = Arrays.copyOf(buf, newSize);

      allocatedSize = newSize;
    }

    return 0;
  }
  

  int put(byte[] data) {
    int len = data.length;
    if (size + len > allocatedSize) {
      if (realloc(size + len) != 0) {
        return -1;
      }
    }

    memcpy(data, len);
    size += len;
    return 0;
  }

  int putc(int v) {
    byte input = (byte) v;
    return putc(input);
  }


  int putc(byte c) {
    byte[] input = {c};
    return put(input);
  }

  int putstr(String str) {
    return put(str.getBytes());
  }

  int putU32(int val) {
    return put(JUtils.intToByteArray(val));
  }

  int putU16(int val) {
    return putU16((short)val);
  }

  int putU16(short val) {
    return put(shortToByteArray(val));
  }

  int putAtom(JSAtom atom) {
    return putU32(atom.getVal());
  }

  int putOpcode(OPCodeEnum opcode) {
    byte[] input = {(byte) opcode.ordinal()};
    return put(input);
  }


  public static byte[] shortToByteArray(short i) {
    byte[] result = new byte[2];
    result[0] = (byte) ((i >> 8) & 0xFF);
    result[1] = (byte) (i & 0xFF);
    return result;
  }
}
