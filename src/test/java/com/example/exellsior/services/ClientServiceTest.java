package com.example.exellsior.services;

import com.example.exellsior.entity.Client;
import com.example.exellsior.entity.Space;
import com.example.exellsior.repository.ClientRepository;
import com.example.exellsior.repository.SpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private VehicleTypeService vehicleTypeService;

    @InjectMocks
    private ClientService clientService;

    @Test
    void resetAllDataResetsOnlyOperationalSpacesAndLinkedClients() {
        Space occupiedSpace = new Space();
        occupiedSpace.setKey("A1");
        occupiedSpace.setOccupied(true);
        occupiedSpace.setHold(true);
        occupiedSpace.setClientId(7L);
        occupiedSpace.setStartTime(12345L);

        Client linkedClient = new Client();
        linkedClient.setId(7L);
        linkedClient.setSpaceKey("A1");
        linkedClient.setEntryTimestamp(new java.util.Date());

        when(spaceRepository.findByOccupiedTrueOrHoldTrueOrClientIdIsNotNullOrStartTimeIsNotNull())
                .thenReturn(List.of(occupiedSpace));
        when(clientRepository.findBySpaceKeyIsNotNullOrIdIn(anyCollection()))
                .thenReturn(List.of(linkedClient));

        clientService.resetAllData();

        assertFalse(occupiedSpace.isOccupied());
        assertFalse(occupiedSpace.isHold());
        assertNull(occupiedSpace.getClientId());
        assertNull(occupiedSpace.getStartTime());
        assertNull(occupiedSpace.getClient());

        assertNull(linkedClient.getSpaceKey());
        assertNull(linkedClient.getEntryTimestamp());
        assertNotNull(linkedClient.getExitTimestamp());
        assertNotNull(linkedClient.getLastDayClosed());

        ArgumentCaptor<List<Space>> spacesCaptor = ArgumentCaptor.forClass(List.class);
        verify(spaceRepository).saveAll(spacesCaptor.capture());
        assertEquals(1, spacesCaptor.getValue().size());
        assertEquals("A1", spacesCaptor.getValue().getFirst().getKey());

        ArgumentCaptor<List<Client>> clientsCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientRepository).saveAll(clientsCaptor.capture());
        assertEquals(1, clientsCaptor.getValue().size());
        assertEquals(7L, clientsCaptor.getValue().getFirst().getId());
    }

    @Test
    void resetAllDataUsesSpaceKeyLookupWhenNoLinkedSpaceIdsExist() {
        Client orphanClient = new Client();
        orphanClient.setId(11L);
        orphanClient.setSpaceKey("B2");
        orphanClient.setExitTimestamp(999L);

        when(spaceRepository.findByOccupiedTrueOrHoldTrueOrClientIdIsNotNullOrStartTimeIsNotNull())
                .thenReturn(List.of());
        when(clientRepository.findBySpaceKeyIsNotNull())
                .thenReturn(List.of(orphanClient));

        clientService.resetAllData();

        assertNull(orphanClient.getSpaceKey());
        assertNull(orphanClient.getEntryTimestamp());
        assertEquals(999L, orphanClient.getExitTimestamp());
        assertNotNull(orphanClient.getLastDayClosed());

        verify(clientRepository).findBySpaceKeyIsNotNull();
        verify(clientRepository, never()).findBySpaceKeyIsNotNullOrIdIn(anyCollection());
    }
}
