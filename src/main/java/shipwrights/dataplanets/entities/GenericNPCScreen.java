package shipwrights.dataplanets.entities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GenericNPCScreen extends Screen {
    private PathfinderMob citizen;
    private Button select1;
    private Button select2;
    private Button select3;
    private Button select4;

    public GenericNPCScreen(Component title, PathfinderMob guildMage) {
        super(title);
        citizen=guildMage;
    }

    public static void open(PathfinderMob mob)
    {
        Minecraft.getInstance().setScreen(new GenericNPCScreen(Component.literal("Neum"),mob));
    }

    //normals: a string array representing the intial screen the player would get by interacting with this citizen
    //0 is citizen intro text, this can be fairly long if you want
    //1-4 is button text, this has to be short, around 20ish characters +/- is the max
    //a hover instance will automatically be created if the button contains (XDu) where it wil show a Dupondius item
    //and whatever X is next to it
    private static final HashMap<String,String[]> normals;
    static
    {
        normals=new HashMap<>();
        normals.put("Neum",new String[]{"Salve @p! welcome to the guild!","What are you baking?","How are you?","Where are you from?","See you later!"});
        normals.put("Publican",new String[]{"Salve @p! welcome to my bar!","What drinks do you have?","How are you?","Where are you from?","See you later!"});
    }



    private String current ="";
    @Override
    protected void init() {
        super.init();
        current = "";
        //this.addButton(new TexturedButtonWidget((width/2)-124,10,248,166,0,0,0,new Identifier("textures/gui/demo_background.png"),f->System.out.println("bg")));
        select1=this.addRenderableWidget(Button.builder(Component.literal("0"),f->buttonPresses(0))
                .bounds((width/2)-120,120,120,20).build());
        select2=this.addRenderableWidget(Button.builder(Component.literal("1"),f->buttonPresses(1))
                .bounds((width/2),120,120,20).build());
        select3=this.addRenderableWidget(Button.builder(Component.literal("2"),f->buttonPresses(2))
                .bounds((width/2)-120,140,120,20).build());
        select4=this.addRenderableWidget(Button.builder(Component.literal("3"),f->buttonPresses(3))
                .bounds((width/2),140,120,20).build());


        String[] an = citizen.getCustomName().getString().split(" "); //John the Baker
        if(an.length>2)
        {
            if(normals.containsKey(an[2]))
            {
                String[] initial = normals.get(an[2]);
                current=initial[0].replace("@p",minecraft.player.getDisplayName().getString());
                select1.setMessage(Component.literal(initial[1].replace("@p",minecraft.player.getName().getString())));
                select2.setMessage(Component.literal(initial[2].replace("@p",minecraft.player.getName().getString())));
                select3.setMessage(Component.literal(initial[3].replace("@p",minecraft.player.getName().getString())));
                select4.setMessage(Component.literal(initial[4].replace("@p",minecraft.player.getName().getString())));
            }
        }




    }

    //fairly simple options system, the text of the option is what is used to determine the option
    //should use the Citizens UUID (specifically the long that is its most significant bits) to generate personal info


    private void buttonPresses(int no)
    {
        String ctext = switch (no) {
            case 0 -> select1.getMessage().getString();
            case 1 -> select2.getMessage().getString();
            case 2 -> select3.getMessage().getString();
            case 3 -> select4.getMessage().getString();
            default -> "";
        };
        System.out.println(ctext);


        switch (ctext) {
            case "How are you?" -> current = "Fine, thank you!";
            case "Where are you from?" -> current = "here";
            case "See you later!", "No thank you" -> minecraft.setScreen(null);
            case "What are you baking?" -> {
                current = "Just got a new batch of bread, would you like some?";
                select1.setMessage(Component.literal("1 loaf please (1Du)"));
                select2.setMessage(Component.literal("½ dozen please (5Du)"));
                select3.setMessage(Component.literal("a dozen please (10Du)"));
                select4.setMessage(Component.literal("No thank you"));
            }
            case "What drinks do you have?" ->
            {
                current = "Currently we have ;n beer on tap!";
                select1.setMessage(Component.literal("One of those please!"));
                select2.setMessage(Component.literal("..."));
                select3.setMessage(Component.literal("..."));
                select4.setMessage(Component.literal("No thank you"));
            }
            case "One of those please!"->
            {


            }
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        minecraft.getTextureManager().bindForSetup(ResourceLocation.parse("textures/gui/demo_background.png"));
        context.blit(ResourceLocation.parse("textures/gui/demo_background.png"),(width/2)-124,20,0,0,248,166);

        super.render(context, mouseX, mouseY, delta);

        int offset = width/2;

        //(DrawContext context, int x, int y, int size, float mouseX, float mouseY, LivingEntity entity)
        InventoryScreen.renderEntityInInventoryFollowsMouse(context,offset-80, 90, 30, offset-80 - mouseX, 40 - mouseY,citizen);


        String[] lines = wrapString(current).split(";n");
        for (int i = 0; i < lines.length; i++) {
            int y = 30+i*10;
            context.drawString(font,lines[i],offset-40,y,0xFFFFFFFF,true);
        }


        children().forEach(element->
        {
            if(element instanceof Button button)
            {
                if(button.getMessage().getString().contains("..."))
                {
                    button.visible=false;
                }
                else
                {
                    button.visible=true;
                }
                if(button.isHovered() && button.getMessage().getString().contains("E"))
                {
                    context.drawString(font,"money",mouseX+21,mouseY+5,0xFFFFFFFF,true);
                }
            }
        });
        
        
    }

    //used mainly in citizen text, formats strings so that they fit on the screen properly
    public static String wrapString(String torap)
    {
        List<String> builder = new ArrayList<>();
        String[] array = torap.split(" ");
        for(String part:array)
        {
            builder.add(part);
        }
        for (int i = 0; i < builder.size(); i+=5) {
            builder.add(i,";n");
        }
        return StringUtils.join(builder," ");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
