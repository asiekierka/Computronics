package pl.asie.computronics.oc.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.EnumSet;

/**
 * @author Vexatos
 */
public class DriverBoardSwitch extends ManagedEnvironment implements RackMountable {

	protected final boolean[] switches = new boolean[4];
	protected final Rack host;
	protected boolean needsUpdate = false;

	public DriverBoardSwitch(Rack host) {
		this.host = host;
		this.setNode(Network.newNode(this, Visibility.Network).
			withComponent("switch_board", Visibility.Network).
			create());
	}

	@Override
	public NBTTagCompound getData() {
		NBTTagCompound tag = new NBTTagCompound();
		byte switchData = 0;
		for(int i = 0; i < switches.length; i++) {
			switchData |= (switches[i] ? 1 : 0) << i;
		}
		tag.setByte("s", switchData);
		return tag;
	}

	// ----------------
	// - 00 00  00 00 -
	// ----------------
	protected static final int[] pixToSwitch = new int[] {
		-1, 0, 0, -1, 1, 1, -1, -1, 2, 2, -1, 3, 3, -1, -1
	};

	@Override
	public boolean onActivate(EntityPlayer player, float hitX, float hitY) {
		int xPix = (int) hitX * 14;
		int yPix = (int) (hitY * 3);
		if(yPix == 3) {
			if(xPix > 0 && xPix < pixToSwitch.length) {
				flipSwitch(pixToSwitch[xPix]);
			}
			return true;
		}
		return false;
	}

	protected void flipSwitch(int i) {
		switches[i] = !switches[i];
	}

	@Override
	public void update() {
		super.update();
		if(needsUpdate) {
			host.markChanged(host.indexOfMountable(this));
			needsUpdate = false;
		}
	}

	public Boolean isActive(int index) {
		return index >= 0 && index < switches.length ? switches[index] : null;
	}

	private void setActive(int index, boolean active) {
		if(switches[index] != active) {
			switches[index] = active;
			needsUpdate = true;
		}
	}

	private int checkSwitch(int index) {
		Boolean active = isActive(index - 1);
		if(active == null) {
			throw new IllegalArgumentException("index out of range");
		}
		return index;
	}

	@Callback(doc = "function(index:number, active:boolean):boolean; Activates or deactivates the specified switch. Returns true on success, false and an error message otherwise", direct = true)
	public Object[] setActive(Context context, Arguments args) {
		int index = checkSwitch(args.checkInteger(0));
		boolean active = args.checkBoolean(1);
		setActive(index, active);
		return new Object[] { true };
	}

	@Callback(doc = "function(index:number):boolean; Returns true if the switch at the specified position is currently active", direct = true)
	public Object[] isActive(Context context, Arguments args) {
		return new Object[] { isActive(checkSwitch(args.checkInteger(0))) };
	}

	@Override
	public void load(NBTTagCompound tag) {
		super.load(tag);
		if(tag.hasKey("s")) {
			byte switchData = tag.getByte("s");
			for(int i = 0; i < switches.length; i++) {
				switches[i] = (switchData & (1 << i)) == 1;
			}
		}
	}

	@Override
	public void save(NBTTagCompound tag) {
		super.save(tag);
		byte switchData = 0;
		for(int i = 0; i < switches.length; i++) {
			switchData |= (switches[i] ? 1 : 0) << i;
		}
		tag.setByte("s", switchData);
	}

	// Unused things

	@Override
	public int getConnectableCount() {
		return 0;
	}

	@Override
	public RackBusConnectable getConnectableAt(int index) {
		return null;
	}

	@Override
	public EnumSet<State> getCurrentState() {
		return EnumSet.noneOf(State.class);
	}
}
