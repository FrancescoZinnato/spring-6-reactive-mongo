package guru.springframework.reactivemongo.services;

import guru.springframework.reactivemongo.domain.Beer;
import guru.springframework.reactivemongo.mappers.BeerMapper;
import guru.springframework.reactivemongo.mappers.BeerMapperImpl;
import guru.springframework.reactivemongo.model.BeerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class BeerServiceImplTest {

    @Autowired
    BeerService beerService;

    @Autowired
    BeerMapper beerMapper;

    BeerDTO beerDTO;

    @BeforeEach
    void setUp() {
        beerDTO = beerMapper.beerToBeerDTO(getTestBeer());
    }

    @Test
    void testFindByBeerStyle() {
        BeerDTO beerDto = getSavedBeerDto();
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        beerService.findByBeerStyle(beerDto.getBeerStyle()).subscribe(dto -> {
            System.out.println(dto.toString());
            atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);
    }

    @Test
    void testFindFirstByBeerName() {
        BeerDTO beerDto = getSavedBeerDto();
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        Mono<BeerDTO> foundDto = beerService.findFirstByBeerName(beerDto.getBeerName());

        foundDto.subscribe(dto -> {
            System.out.println("Found beer: " + dto.toString());
            atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);
    }

    @Test
    @DisplayName("Test Update Beer Using Block")
    void testUpdateBlocking() {
        final String newName = "New Beer Name";  // use final so cannot mutate
        BeerDTO savedBeerDto = getSavedBeerDto();
        savedBeerDto.setBeerName(newName);

        BeerDTO updatedDto = beerService.saveBeer(Mono.just(savedBeerDto)).block();

        //verify exists in db
        BeerDTO fetchedDto = beerService.getById(updatedDto.getId()).block();
        assertThat(fetchedDto.getBeerName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Test Update Using Reactive Streams")
    void testUpdateStreaming() {
        final String newName = "New Beer Name";  // use final so cannot mutate

        AtomicReference<BeerDTO> atomicDto = new AtomicReference<>();

        beerService.saveBeer(Mono.just(getTestBeerDto()))
                .map(savedBeerDto -> {
                    savedBeerDto.setBeerName(newName);
                    return savedBeerDto;
                })
                .flatMap(beerService::saveBeer) // save updated beer
                .flatMap(savedUpdatedDto -> beerService.getById(savedUpdatedDto.getId())) // get from db
                .subscribe(dtoFromDb -> {
                    System.out.println("Updated beer with ID: " + dtoFromDb.getId());
                    atomicDto.set(dtoFromDb);
                });

        await().until(() -> atomicDto.get() != null);
        assertThat(atomicDto.get().getBeerName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Test Save Beer Using Subscriber")
    void testSaveBeerUsingSubscriber() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        AtomicReference<BeerDTO> atomicReference = new AtomicReference<>();

        Mono<BeerDTO> monoDto = beerService.saveBeer(Mono.just(beerDTO));

        monoDto.subscribe(savedDto -> {
            System.out.println("Saved BeerDTO with ID: " + savedDto.getId());
            atomicBoolean.set(true);
            atomicReference.set(savedDto);
        });

        await().untilTrue(atomicBoolean);

        BeerDTO persistedBeerDTO = atomicReference.get();
        assertThat(persistedBeerDTO).isNotNull();
        assertThat(persistedBeerDTO.getId()).isNotNull();
    }

    @Test
    @DisplayName("Test Save Beer Using Block")
    void testSaveBeerUsingBlock() {
        BeerDTO savedDto = beerService.saveBeer(Mono.just(beerDTO)).block();
        assertThat(savedDto).isNotNull();
        assertThat(savedDto.getId()).isNotNull();
    }

    @Test
    void testDeleteBeer() {
        BeerDTO beerToDelete = getSavedBeerDto();

        beerService.deleteBeerById(beerToDelete.getId()).block();

        Mono<BeerDTO> emptyBeerMono = beerService.getById(beerToDelete.getId());

        BeerDTO emptyDto = emptyBeerMono.block();

        assertThat(emptyDto).isNull();
    }

    @Test
    void testSaveBeer() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        savedMono.subscribe(savedDTO -> {
            System.out.println("Beer Saved with ID: " + savedDTO.getId());
            atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);
    }

    public BeerDTO getSavedBeerDto() {
        return beerService.saveBeer(Mono.just(getTestBeerDto())).block();
    }

    public static BeerDTO getTestBeerDto() {
        return new BeerMapperImpl().beerToBeerDTO(getTestBeer());
    }

    public static Beer getTestBeer() {
        return Beer.builder()
                .beerName("Space Dust")
                .beerStyle("IPA")
                .price(BigDecimal.TEN)
                .quantityOnHand(12)
                .upc("123213")
                .build();
    }


}