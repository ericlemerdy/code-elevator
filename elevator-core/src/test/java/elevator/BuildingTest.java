package elevator;

import elevator.engine.ElevatorEngine;
import elevator.exception.ElevatorIsBrokenException;
import elevator.user.ConstantMaxNumberOfUsers;
import elevator.user.DeterministicUser;
import elevator.user.RandomUser;
import elevator.user.User;
import org.fest.assertions.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Set;

import static elevator.Command.*;
import static elevator.engine.ElevatorEngine.LOWER_FLOOR;
import static elevator.engine.assertions.Assertions.assertThat;
import static java.lang.Boolean.TRUE;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuildingTest {

    @Mock
    public ElevatorEngine elevator;

    @Rule
    public ExpectedException expectedException = none();

    @Test
    public void should_add_user() {
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());

        building.addUser(new RandomUser());

        assertThat(building).users().hasSize(1);
    }

    @Test
    public void should_enter_to_elevator_if_doors_are_open_and_at_floor_when_creating() {
        when(elevator.nextCommand()).thenReturn(OPEN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());
        building.updateBuildingState();

        building.addUser(new DeterministicUser(0, 1));

        assertThat(building.travelingUsers()).isEqualTo(1);
    }

    @Test
    public void should_not_add_more_than_max_number_of_users() {
        final ConstantMaxNumberOfUsers maxNumberOfUsers = new ConstantMaxNumberOfUsers();
        final Integer MAX_NUMBER_OF_USERS = maxNumberOfUsers.value();
        final Building building = new Building(elevator, maxNumberOfUsers);
        for (Integer i = 1; i <= MAX_NUMBER_OF_USERS; i++) {
            building.addUser(new RandomUser());
        }
        assertThat(building).users().hasSize(MAX_NUMBER_OF_USERS);

        building.addUser(new RandomUser());

        assertThat(building).users().hasSize(MAX_NUMBER_OF_USERS);
    }

    @Test
    public void should_set_max_number_of_users_to_zero_if_max_number_of_users_is_negative() {
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers(-4));

        building.addUser(new RandomUser());

        assertThat(building).users().hasSize(0);
    }

    @Test
    public void should_have_an_initial_state() throws Exception {
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());

        assertThat(building).doorIs(Door.CLOSE).floorIs(LOWER_FLOOR);
    }

    @Test
    public void should_restore_initial_state_if_reseted() throws Exception {
        when(elevator.nextCommand()).thenReturn(UP);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());
        building.updateBuildingState();
        building.addUser(new RandomUser());

        building.reset();

        assertThat(building).doorIs(Door.CLOSE).floorIs(LOWER_FLOOR).users().isEmpty();
    }

    @Test
    public void should_does_nothing_if_elevator_does_nothing() {
        when(elevator.nextCommand()).thenReturn(NOTHING);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());

        building.updateBuildingState();

        assertThat(building).doorIs(Door.CLOSE).floorIs(LOWER_FLOOR);
    }

    @Test
    public void should_throws_exception_if_elevator_closes_doors_but_doors_are_already_closed() {
        when(elevator.nextCommand()).thenReturn(CLOSE);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());

        expectedException.expect(ElevatorIsBrokenException.class);
        expectedException.expectMessage("can't close doors");
        building.updateBuildingState();
    }

    @Test
    public void should_throw_exception_if_elevator_opens_doors_but_doors_are_already_open() {
        when(elevator.nextCommand()).thenReturn(OPEN, OPEN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());
        building.updateBuildingState();

        expectedException.expect(ElevatorIsBrokenException.class);
        expectedException.expectMessage("can't open doors because they aren't closed");
        building.updateBuildingState();
    }

    @Test
    public void should_throws_exception_if_elevator_goes_down_but_is_already_at_lower_floor() {
        when(elevator.nextCommand()).thenReturn(DOWN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());

        expectedException.expect(ElevatorIsBrokenException.class);
        expectedException.expectMessage("can't go down because current floor is the lowest floor");
        building.updateBuildingState();
    }

    @Test
    public void should_throws_exception_if_elevator_goes_down_but_doors_are_open() {
        when(elevator.nextCommand()).thenReturn(UP, Command.OPEN, DOWN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());
        building.updateBuildingState();
        building.updateBuildingState();

        expectedException.expect(ElevatorIsBrokenException.class);
        expectedException.expectMessage("can't go down because doors are opened");
        building.updateBuildingState();
    }

    @Test
    public void should_throws_exception_if_elevator_goes_up_but_is_already_at_higher_floor() {
        when(elevator.nextCommand()).thenReturn(UP, UP, UP, UP, UP, UP);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());
        building.updateBuildingState();
        building.updateBuildingState();
        building.updateBuildingState();
        building.updateBuildingState();
        building.updateBuildingState();

        expectedException.expect(ElevatorIsBrokenException.class);
        expectedException.expectMessage("can't go up because current floor is the highest floor");
        building.updateBuildingState();
    }

    @Test
    public void should_throws_exception_if_elevator_goes_up_but_doors_are_open() {
        when(elevator.nextCommand()).thenReturn(Command.OPEN, UP);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers());
        building.updateBuildingState();

        expectedException.expect(ElevatorIsBrokenException.class);
        expectedException.expectMessage("can't go up because doors are opened");
        building.updateBuildingState();
    }

    @Test
    public void shoud_count_traveling_users() {
        when(elevator.nextCommand()).thenReturn(OPEN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers())
                .addUser(new DeterministicUser(0, 3))
                .addUser(new DeterministicUser(5, 3));
        building.updateBuildingState();

        int travelingUsers = building.travelingUsers();

        assertThat(travelingUsers).isEqualTo(1);
    }

    @Test
    public void should_know_how_many_users_are_waiting() {
        Integer unsignifiantFloorToGo = 3;
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers()).
                addUser(new DeterministicUser(0, unsignifiantFloorToGo)).
                addUser(new DeterministicUser(5, unsignifiantFloorToGo)).
                addUser(new DeterministicUser(5, unsignifiantFloorToGo));

        int[] waitingUsersByFloor = building.waitingUsersByFloors();

        assertThat(waitingUsersByFloor).isEqualTo(new int[]{
                1, 0, 0, 0, 0, 2
        });
    }

    @Test
    public void should_get_all_waiting_users() {
        when(elevator.nextCommand()).thenReturn(Command.OPEN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers()).
                addUser(new DeterministicUser(0, 1)).
                addUser(new DeterministicUser(1, 2)).
                addUser(new DeterministicUser(1, 3));
        building.updateBuildingState();

        Set<User> waitingUsers = building.waitingUsers();

        assertThat(waitingUsers).satisfies(new AllUsersAreWaiting());
    }

    @Test
    public void should_get_floor_button_states_in_elevator() {
        when(elevator.nextCommand()).thenReturn(OPEN);
        Building building = new Building(elevator, new ConstantMaxNumberOfUsers()).
                addUser(new DeterministicUser(0, 3)).
                addUser(new DeterministicUser(0, 4)).
                addUser(new DeterministicUser(1, 5));
        building.updateBuildingState();

        boolean[] floorButtonStatesInElevator = building.getFloorButtonStatesInElevator();

        assertThat(floorButtonStatesInElevator).isEqualTo(new boolean[]{
                false,
                false,
                false,
                true,
                true,
                false
        });
    }

    private static class AllUsersAreWaiting extends Condition<Collection<?>> {

        @Override
        public boolean matches(Collection<?> users) {
            Boolean allUsersAreWaiting = TRUE;
            for (Object user : users) {
                allUsersAreWaiting &= ((User) user).waiting();
            }
            return allUsersAreWaiting;
        }

    }

}
