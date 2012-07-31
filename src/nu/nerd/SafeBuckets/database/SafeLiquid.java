package nu.nerd.SafeBuckets.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name = "safeliquids")
public class SafeLiquid {
	
	@Id
	private int id;
	
	@NotNull
	private int x;
	private int y;
	private int z;
	private String world;

	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}

	public void setWorld(String world) {
		this.world = world;
	}

	public String getWorld() {
		return world;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getZ() {
		return z;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getY() {
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getX() {
		return x;
	}
}
