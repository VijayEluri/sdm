package dk.trifork.sdm.spooler;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpoolerManagerTest {
    private static final String TMP_FILE_SPOOLER_MANAGER = "/tmp/FileSpoolerManager";

    @Test
    public void testInit() {
        SpoolerManager fsm = new SpoolerManager(TMP_FILE_SPOOLER_MANAGER);
        FileSpoolerImpl spooler = fsm.spoolers.get("takst");
        assertNotNull(spooler);
    }

    @Test
    public void testUri2filepath() {
        String uri = "file:///testdir/testfile";
        String filepath = "/testdir/testfile";
        assertEquals(filepath, SpoolerManager.uri2filepath(uri));

        uri = "ftp:///testdir/testfile";
        assertNull(SpoolerManager.uri2filepath(uri));

        uri = "/testdir/testfile";
        assertNull(SpoolerManager.uri2filepath(uri));

        uri = ":¡@$£@½$";
        assertNull(SpoolerManager.uri2filepath(uri));
    }

    @Test
    public void testAreAllSpoolersRunning() {
        SpoolerManager fsm = new SpoolerManager(TMP_FILE_SPOOLER_MANAGER);
        fsm.spoolers = new HashMap<String, FileSpoolerImpl>();

        // Add a mocked running spooler
        FileSpoolerImpl mock1 = mock(FileSpoolerImpl.class);
        when(mock1.getStatus()).thenReturn(FileSpoolerImpl.Status.RUNNING);
        fsm.spoolers.put("takst", mock1);

        assertTrue(fsm.isAllSpoolersRunning());

        JobSpoolerImpl mock2 = mock(JobSpoolerImpl.class);
        when(mock2.getStatus()).thenReturn(JobSpoolerImpl.Status.RUNNING);
        fsm.jobSpoolers.put("navnebeskyttelse", mock2);
        
        assertTrue(fsm.isAllSpoolersRunning());

        // And a spooler that is not runnning
        when(mock1.getStatus()).thenReturn(FileSpoolerImpl.Status.ERROR);
        fsm.spoolers.put("test2", mock1);
        assertFalse(fsm.isAllSpoolersRunning());
    }

    @After
    public void cleanUpfiles() {
        FileSpoolerImplTest.deleteFile(new File(TMP_FILE_SPOOLER_MANAGER));
    }

}