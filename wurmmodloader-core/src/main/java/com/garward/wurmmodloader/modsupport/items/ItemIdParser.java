package com.garward.wurmmodloader.modsupport.items;

import com.garward.wurmmodloader.modsupport.IdType;
import com.garward.wurmmodloader.modsupport.NonFreezingNamedIdParser;

/**
 * Parse a list of item names and ids.
 */
public class ItemIdParser extends NonFreezingNamedIdParser {
	
	@Override
	protected String getNamesClassName() {
		return "com.wurmonline.server.items.ItemList";
	}
	
	@Override
	protected IdType getIdFactoryType() {
		return IdType.ITEMTEMPLATE;
	}
		
	@Override
	protected int unparsable(String name) {
		throw new IllegalArgumentException(name + " is not a valid item name");
	}
}
