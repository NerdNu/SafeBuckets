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
	private Long hash;
	private String world;

	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}

	public void setHash(Long hash) {
		this.hash = hash;
	}

	public Long getHash() {
		return hash;
	}

	public void setWorld(String world) {
		this.world = world;
	}

	public String getWorld() {
		return world;
	}
}
