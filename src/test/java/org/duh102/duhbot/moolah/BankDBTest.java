package org.duh102.duhbot.moolah;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BankDBTest {
  @Test
  public void testGetInstance() throws Exception {
    BankDB memoryDB = BankDB.getMemoryInstance();
  }
  @Test
  public void testGetInstanceStatic() throws Exception {
    BankDB memoryDB = BankDB.getMemoryInstance();
    BankDB memoryDB2 = BankDB.getMemoryInstance();
    assertSame(memoryDB, memoryDB2);
  }
}
