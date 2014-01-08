package org.gbif.checklistbank.col;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ColAnnotationImportTest {

  @Test
  public void testIsRejected() throws Exception {
    assertTrue(ColAnnotationImport.isRejected("rejected:\"Acacia guanacastensis (H.D.Clarke, Seigler & Ebinger) Ebinger & Seigler, name missing in ILDIS. Basionym: Acacia farnesiana (L.) Willd. var. guanacastensis H.D.Clarke , Seigler & Ebinger already in ILDIS.\":Others"));
    assertTrue(ColAnnotationImport.isRejected(" REJECTED:Name with unresolved nomenclatural status"));
    assertTrue(ColAnnotationImport.isRejected("Rejected:Name with unresolved nomenclatural status"));
    assertFalse(ColAnnotationImport.isRejected("placed:Acacia granulosa Labill. - basionym of Archidendropsis granulosa (Labill.) I.C.Nielsen:Incomplete name"));
  }
}
