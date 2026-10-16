package com.zpantry.integration.ai;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class AiServiceKeepAliveSchedulerTest {
    @Test
    void probesTheAiService() {
        AiServiceHealthProbe probe = mock(AiServiceHealthProbe.class);

        new AiServiceKeepAliveScheduler(probe).pingAiService();

        verify(probe).probe();
    }

    @Test
    void absorbsAProbeFailureSoFutureHeartbeatsCanRun() {
        AiServiceHealthProbe probe = mock(AiServiceHealthProbe.class);
        doThrow(new IllegalStateException("offline")).when(probe).probe();

        new AiServiceKeepAliveScheduler(probe).pingAiService();

        verify(probe).probe();
    }
}
