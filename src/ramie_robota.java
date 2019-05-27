import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import static java.lang.Math.*;
import javax.media.j3d.*;
import javax.swing.Timer;
import javax.vecmath.*;

/**
 *
 * @author Maciej Szpiech
 * 
 * sterowanie:
 *      [W] - podniesienie ramienia
 *      [S] - opuszczenie ramienia
 *      [A] - obrót ramienia przeciwnie do ruchu wskazówek zegara
 *      [D] - obrót zgodnie z ruchem wskazówek zegara
 *      [Q] - wydłużenie wysięgnika
 *      [E] - skrócenie wysięgnika
 *      [R] - złapanie kostki
 *      [T] - przywrócenie widoku początkowego
 * 
**/
public class ramie_robota extends Applet implements ActionListener, KeyListener {
    
    private CollisionDetector kolizja;
    private SimpleUniverse universe;
    private BranchGroup bran_kostki;
    private BranchGroup graf_sceny;
    private Button start_button = new Button("Start");
    private Button save_button = new Button("Zapisz");
    private Button read_button = new Button("Odtworz");
    private Button clear_button = new Button("Czysc zapis");
    private Timer timer;
    private TransformGroup pozycja_ogolna;
    private TransformGroup pozycja_ramienia;
    private TransformGroup rotacja_ramienia;
    private TransformGroup pozycja_wysiegnika;
    private TransformGroup rotacja_wysiegnika;    
    private TransformGroup pozycja_kostki;    
    private TransformGroup pozycja_chwytaka_2;    
    private Transform3D rotacja_kos = new Transform3D();    
    private Transform3D rotacja_ram = new Transform3D();
    private Transform3D ruch_ram = new Transform3D();
    private Transform3D rotacja_wys = new Transform3D();
    private Transform3D ruch_wys = new Transform3D();   
    private Transform3D przesuniecie_obserwatora = new Transform3D();
    private Transform3D spadanie = new Transform3D(); 
    private Transform3D ostatnie_polozenie_kostki = new Transform3D(); 
    private BoundingSphere sfera = new BoundingSphere(new Point3d(0,0,0),100);
    private boolean key_w ,key_s, key_a, key_d, key_q, key_e, key_r, key_f= false;
    private boolean flaga_schwytania, flaga_opuszczenia, odczyt_flaga, zapis_flaga = false;
    private boolean[][] ruchy;
    private float wysokosc, dlugosc, ostatnia_dlugosc, ostatnia_wysokosc= 0.0f;
    private double kat, ostatni_kat = 0;
    private int zapis_ruchu, odczyt_ruchu, pomocnicza_ruchu = 0;
    private Box wysiegnik_2;
    private Shape3D kostka;  
    private Vector3f pozycja_spadania = new Vector3f();
    private Vector3f ostatnia_pozycja = new Vector3f(-0.45f, -0.25f, 0.3f);
    
    //-------------------------------------
    //               MAIN
    //-------------------------------------
    public ramie_robota(){
        setLayout (new BorderLayout());
        
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);
        add(BorderLayout.CENTER, canvas);        
        canvas.addKeyListener(this);
        
        timer = new Timer(10, this);    // częstotliwość odświeżania
        
        ruchy = new boolean[7][12000];  // zmienna zapamiętująca ruchy
        for(int i=0; i<7; i++) for(int j=0; j<12000; j++) ruchy[i][j] = false;
        
        // panel w dolnej części okna wraz z przyciskami 
        Panel panel = new Panel();   
        add("South",panel);
        
        panel.add(start_button);
        start_button.addActionListener(this);
        start_button.addKeyListener(this);
        start_button.setBackground(Color.green);
        start_button.setPreferredSize(new Dimension(100, 35));
        
        panel.add(save_button);
        save_button.addActionListener(this);
        save_button.addKeyListener(this);
        save_button.setBackground(Color.green);
        save_button.setPreferredSize(new Dimension(100, 35));
        
        panel.add(read_button);
        read_button.addActionListener(this);
        read_button.addKeyListener(this);
        read_button.setBackground(Color.green);
        read_button.setPreferredSize(new Dimension(100, 35));
        
        panel.add(clear_button);
        clear_button.addActionListener(this);
        clear_button.addKeyListener(this);
        clear_button.setPreferredSize(new Dimension(100, 35));
        
        // ustawienie początkowego dostępu do przycisków
        save_button.setEnabled(false);
        read_button.setEnabled(false);
        clear_button.setEnabled(false);
        
        BranchGroup scena = utworz_scene();     // scena główna
        universe = new SimpleUniverse(canvas);  
        
        // sterowanie kamerą za pomocą myszki       
        OrbitBehavior orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
        orbit.setTranslateEnable(false);      
        orbit.setSchedulingBounds(sfera);
        orbit.setRotXFactor(2);
        orbit.setRotYFactor(2);
        ViewingPlatform vp = universe.getViewingPlatform();
        vp.setViewPlatformBehavior(orbit);
        
        // ustawienie kamery
        przesuniecie_obserwatora.set(new Vector3f(0.0f, 0.6f, 3.0f));
        Transform3D rot_obs = new Transform3D();
        rot_obs.rotX((float)(-Math.PI/16));
        przesuniecie_obserwatora.mul(rot_obs);
        
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(przesuniecie_obserwatora);
        universe.addBranchGraph(scena);
    }
    
    //  zaimplementowanie wszystkich elementów
    //
    public BranchGroup utworz_scene(){       
        // gałąź główna sceny wraz z ustawieniem uprawnień
        graf_sceny = new BranchGroup();
        graf_sceny.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        graf_sceny.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        graf_sceny.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE); 
        graf_sceny.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        graf_sceny.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        pozycja_ogolna = new TransformGroup();
        graf_sceny.addChild(pozycja_ogolna);

        // tło programu
        graf_sceny.addChild(background());
        
        // światla
        graf_sceny.addChild(swiatlo_z_kierunkiem()); 
        graf_sceny.addChild(swiatlo_bezkierunkowe());
        
        // elementy
        podloze( wyglad_morza(), wyglad_podloza());
        cylinder(wyglad_cylindra());
        ramie(wyglad_ramienia());
        wysiegnik(wyglad_wysiegnika());
        chwytak(wyglad_chwytaka());
        kostka();
        
        graf_sceny.compile();
        
        return graf_sceny;
    }
    
    //  tekstura tła
    //
    public Background background(){
        TextureLoader tekstura = new TextureLoader("sky_texture.jpg", "NIEBO",
                TextureLoader.BY_REFERENCE | TextureLoader.Y_UP, this);
        Background background = new Background(new Color3f(0.196078f, 0.6f, 0.8f));
        BoundingSphere background_bounds = new BoundingSphere(new Point3d(0,0,0), 110);
        background.setApplicationBounds(background_bounds);
           
	BranchGroup bran_background = new BranchGroup();        
        Sphere tlo = new Sphere(1f, Sphere.GENERATE_NORMALS |
                Sphere.GENERATE_NORMALS_INWARD |
                Sphere.GENERATE_TEXTURE_COORDS, 50);  
        
        Appearance wyglad_background = tlo.getAppearance();
        wyglad_background.setTexture(tekstura.getTexture());     
        bran_background.addChild(tlo);
        background.setGeometry(bran_background);
        
        return background;
    }
    
    //  światła
    //
    public DirectionalLight swiatlo_z_kierunkiem(){
        Color3f swiatlo1 = new Color3f(0.5f, 0.5f, 0.5f);
        Vector3f kierunek_swiatla = new Vector3f(0.4f, -3.0f, -12.0f);
        DirectionalLight swiatlo_z_kierunkiem = new DirectionalLight(swiatlo1, kierunek_swiatla);
        swiatlo_z_kierunkiem.setInfluencingBounds(sfera);
        
        return swiatlo_z_kierunkiem;
    } 
    public AmbientLight swiatlo_bezkierunkowe(){
        Color3f swiatlo2 = new Color3f(0.2f, 0.2f, .0f);
        AmbientLight swiatlo_bezkierunkowe = new AmbientLight(swiatlo2);
        swiatlo_bezkierunkowe.setInfluencingBounds(sfera);
        
        return swiatlo_bezkierunkowe;
    }
    
    //-------------------------------------
    //              ELEMENTY
    //-------------------------------------
    //  morze + wyspa
    //
    public void podloze(Appearance morze, Appearance trawa){
        Cylinder podloga_1 = new Cylinder(10.0f, 0.08f, Cylinder.GENERATE_TEXTURE_COORDS
              | Cylinder.GENERATE_NORMALS, 40, 20,morze);
        
        TransformGroup pozycja_podloza_1 = new TransformGroup();
        
        Transform3D pozycja_0_a = new Transform3D();
        pozycja_0_a.setTranslation(new Vector3f(0.0f,-0.35f,0.0f));
        
        pozycja_podloza_1.setTransform(pozycja_0_a);
        pozycja_podloza_1.addChild(podloga_1);
        pozycja_ogolna.addChild(pozycja_podloza_1);
        //---------------------------
        Cylinder podloga_2 = new Cylinder(0.6f, 0.005f, Cylinder.GENERATE_TEXTURE_COORDS
              | Cylinder.GENERATE_NORMALS, 40, 20,trawa);
        
        TransformGroup pozycja_podloza_2 = new TransformGroup(); 
        
        Transform3D pozycja_0_b = new Transform3D();
        pozycja_0_b.setTranslation(new Vector3f(0.0f,-0.312f,0.0f));
        
        pozycja_podloza_2.setTransform(pozycja_0_b);
        pozycja_podloza_2.addChild(podloga_2);
        pozycja_ogolna.addChild(pozycja_podloza_2);
    }
    
    //  główny cylinder, trzon robota
    //
    public void cylinder(Appearance wyglad){
        Cylinder cylinder = new Cylinder(0.03f, 0.6f, Cylinder.GENERATE_TEXTURE_COORDS
              | Cylinder.GENERATE_NORMALS, 40, 20,wyglad);
        
        TransformGroup pozycja_cylindra = new TransformGroup();
        pozycja_cylindra.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); 
        
        Transform3D pozycja_1 = new Transform3D();
        pozycja_1.setTranslation(new Vector3f(0.0f,0.0f,0.0f));
        
        pozycja_cylindra.setTransform(pozycja_1);
        pozycja_cylindra.addChild(cylinder);
        pozycja_ogolna.addChild(pozycja_cylindra);
    }
    
    //  ramię przyczepione do trzonu
    //  
    public void ramie(Appearance wyglad){      
        Box ramie = new Box(0.05f,0.02f,0.1f,Box.GENERATE_NORMALS|Box.GENERATE_TEXTURE_COORDS,wyglad);
        
        rotacja_ramienia = new TransformGroup();
        rotacja_ramienia.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);        
        pozycja_ramienia = new TransformGroup();
        pozycja_ramienia.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        Transform3D pozycja_2 = new Transform3D();
        pozycja_2.setTranslation(new Vector3f(0.0f,0.0f,0.05f));
        
        rotacja_ramienia.setTransform(pozycja_2);   
        pozycja_ramienia.addChild(ramie);
        rotacja_ramienia.addChild(pozycja_ramienia);
        pozycja_ogolna.addChild(rotacja_ramienia);
    }    
    
    //  wysięgnik wraz z podstawką do chwytaka
    //
    public void wysiegnik(Appearance wyglad){      
        Cylinder wysiegnik_1 = new Cylinder(0.015f, 0.5f, Cylinder.GENERATE_TEXTURE_COORDS
              | Cylinder.GENERATE_NORMALS, 40, 20,wyglad);
        
        rotacja_wysiegnika = new TransformGroup();
        rotacja_wysiegnika.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);        
        pozycja_wysiegnika = new TransformGroup();       
        pozycja_wysiegnika.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        pozycja_wysiegnika.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        pozycja_wysiegnika.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);   
        pozycja_wysiegnika.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        pozycja_wysiegnika.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); 

        Transform3D pozycja_3_a = new Transform3D();
        pozycja_3_a.setTranslation(new Vector3f(0.0f,wysokosc,0.05f));

        rotacja_wys.rotZ(1.57);      
        pozycja_3_a.mul(rotacja_wys);
        
        rotacja_wysiegnika.setTransform(pozycja_3_a);   
        pozycja_wysiegnika.addChild(wysiegnik_1);
        rotacja_wysiegnika.addChild(pozycja_wysiegnika);
        pozycja_ramienia.addChild(rotacja_wysiegnika);
        //-----------------------
        wysiegnik_2 = new Box(0.01f,0.02f,0.035f,Box.GENERATE_NORMALS|Box.GENERATE_TEXTURE_COORDS,wyglad);
        
        TransformGroup rotacja_wysiegnika_doczep = new TransformGroup();
        rotacja_wysiegnika_doczep.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);        
        TransformGroup pozycja_wysiegnika_doczep = new TransformGroup();
        pozycja_wysiegnika_doczep.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        Transform3D pozycja_3_b = new Transform3D();
        pozycja_3_b.setTranslation(new Vector3f(0.0f,wysokosc+0.25f,0.0f));
        pozycja_3_b.mul(rotacja_wys);
        
        rotacja_wysiegnika_doczep.setTransform(pozycja_3_b);
        pozycja_wysiegnika_doczep.addChild(wysiegnik_2);
        rotacja_wysiegnika_doczep.addChild(pozycja_wysiegnika_doczep);
        pozycja_wysiegnika.addChild(rotacja_wysiegnika_doczep);
        
    }
    
    //  chwytak przyczenipony ddo wysięgnika
    //
    public void chwytak(Appearance wyglad){           
        Box chwytak_1 = new Box(0.02f,0.02f,0.005f,Box.GENERATE_NORMALS|Box.GENERATE_TEXTURE_COORDS,wyglad);
        
        TransformGroup rotacja_chwytaka_1 = new TransformGroup();
        rotacja_chwytaka_1.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);        
        TransformGroup pozycja_chwytaka_1 = new TransformGroup();
        pozycja_chwytaka_1.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        Transform3D pozycja_4_a = new Transform3D();
        pozycja_4_a.setTranslation(new Vector3f(0.0f,wysokosc+0.28f,-0.03f));
     
        rotacja_chwytaka_1.setTransform(pozycja_4_a);
        pozycja_chwytaka_1.addChild(chwytak_1);
        rotacja_chwytaka_1.addChild(pozycja_chwytaka_1);
        pozycja_wysiegnika.addChild(rotacja_chwytaka_1);
        //-------------------------------
        Box chwytak_2 = new Box(0.02f,0.02f,0.005f,Box.GENERATE_NORMALS|Box.GENERATE_TEXTURE_COORDS,wyglad);
        
        TransformGroup rotacja_chwytaka_2 = new TransformGroup();
        rotacja_chwytaka_2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);        
        pozycja_chwytaka_2 = new TransformGroup();
        pozycja_chwytaka_2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
           
        Transform3D pozycja_4_b = new Transform3D();
        pozycja_4_b.setTranslation(new Vector3f(0.0f,wysokosc+0.28f,0.03f));
             
        rotacja_chwytaka_2.setTransform(pozycja_4_b);                
        pozycja_chwytaka_2.addChild(chwytak_2);            
        rotacja_chwytaka_2.addChild(pozycja_chwytaka_2);
        pozycja_wysiegnika.addChild(rotacja_chwytaka_2);
    }
    
    //  kostka, element przenoszony
    //
    public void kostka(){           
        kostka = new pudelko(0.1, 0.1, 0.1);  // obiekt Shape3D
        
        //  nowy branchgroup umożliwiający przełączanie połącznenia kostki z elementami
        bran_kostki = new BranchGroup();  
        bran_kostki.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        bran_kostki.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        bran_kostki.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);     
        bran_kostki.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        bran_kostki.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        pozycja_kostki = new TransformGroup();
        pozycja_kostki.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        Transform3D pozycja_5 = new Transform3D();
        pozycja_5.setTranslation(new Vector3f(-0.45f, -0.25f, 0.3f));
        rotacja_kos.rotY(0.61f);
        pozycja_5.mul(rotacja_kos);
        
        Appearance wyglad = kostka.getAppearance();
        ColoringAttributes ca = new ColoringAttributes();
	ca.setColor(1.0f, 0.3f, 0.0f);  
        wyglad.setCapability(wyglad.ALLOW_COLORING_ATTRIBUTES_WRITE);
        wyglad.setColoringAttributes(ca);
        
        //  system kolizyjny przypisany do kostki
        kolizja = new CollisionDetector(kostka);
	BoundingSphere bounds =
	    new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
	kolizja.setSchedulingBounds(bounds);
        
        pozycja_kostki.addChild(kolizja);
        pozycja_kostki.setTransform(pozycja_5);
        pozycja_kostki.addChild(kostka);
        bran_kostki.addChild(pozycja_kostki);
        graf_sceny.addChild(bran_kostki);
    }
    
    //-------------------------------------
    //          WYGLAD ELEMENTOW
    //-------------------------------------
    //
    public Appearance wyglad_podloza(){
        Appearance wyglad_podloza = new Appearance();
        
        Material material = new Material();
        material.setEmissiveColor( 0.0f, 0.76f, 0.0f ); // kolor bryly
        
        TextureLoader loader = new TextureLoader("grass.jpg",
                                "LUMINANCE", new Container());
        Texture texture = loader.getTexture();
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.BLEND);

        wyglad_podloza.setTexture(texture);
        wyglad_podloza.setTextureAttributes(texAttr);
        
        wyglad_podloza.setMaterial(material);
        
        return wyglad_podloza;
    }   
    public Appearance wyglad_morza(){
        Appearance wyglad_morza = new Appearance();
        
        Material material = new Material();
        material.setEmissiveColor(0.24902f, 0.347059f, 0.847059f);
        
        TextureLoader loader = new TextureLoader("water.jpg",
                                "LUMINANCE", new Container());
        Texture texture = loader.getTexture();
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.BLEND);

        wyglad_morza.setTexture(texture);
        wyglad_morza.setTextureAttributes(texAttr);
        
        wyglad_morza.setMaterial(material);
        
        return wyglad_morza;
    }
    
    public Appearance wyglad_cylindra(){
        Appearance wyglad_cylindra = new Appearance();
        
        Material material = new Material();
        material.setEmissiveColor( 0.3f, 0.3f, 0.3f ); // kolor bryly
        material.setDiffuseColor( 0.3f, 0.3f, 0.3f);  // kolor odbicia
        material.setSpecularColor( 0.7f, 0.7f, 0.7f ); // kolor swiatla na bryle
        material.setShininess(50f);
        
        TextureLoader loader = new TextureLoader("metal2.jpg",
                                "LUMINANCE", new Container());
        Texture texture = loader.getTexture();
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.MODULATE);

        wyglad_cylindra.setTexture(texture);
        wyglad_cylindra.setTextureAttributes(texAttr);
        
        wyglad_cylindra.setMaterial(material);
        
        return wyglad_cylindra;
    }
    
    public Appearance wyglad_ramienia(){
        Appearance wyglad_ramienia = new Appearance();
        
        Material material = new Material();
        material.setEmissiveColor( 0.6f, 0.6f, 0.6f );
        material.setDiffuseColor( 0.3f, 0.3f, 0.3f);
        material.setSpecularColor( 0.5f, 0.5f, 0.5f );
        material.setShininess(50f);
        
        wyglad_ramienia.setMaterial(material);

        TextureLoader loader = new TextureLoader("metal1.png",
                                "LUMINANCE", new Container());
        Texture texture = loader.getTexture();
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.MODULATE);

        wyglad_ramienia.setTexture(texture);
        wyglad_ramienia.setTextureAttributes(texAttr);
        
        return wyglad_ramienia;
    }
      
    public Appearance wyglad_wysiegnika(){
        Appearance wyglad_wysiegnika = new Appearance();
        
        Material material = new Material();
        material.setEmissiveColor( 0.3f, 0.3f, 0.3f );
        material.setDiffuseColor( 0.3f, 0.3f, 0.3f);
        material.setSpecularColor( 0.5f, 0.5f, 0.5f );
        material.setShininess(50f);
        
        wyglad_wysiegnika.setMaterial(material);
        
        TextureLoader loader = new TextureLoader("metal2.jpg",
                                "LUMINANCE", new Container());
        Texture texture = loader.getTexture();
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.MODULATE);

        wyglad_wysiegnika.setTexture(texture);
        wyglad_wysiegnika.setTextureAttributes(texAttr);
        
        return wyglad_wysiegnika;
    }
    
    public Appearance wyglad_chwytaka(){
        Appearance wyglad_chwytaka = new Appearance();
        
        Material material = new Material();
        material.setEmissiveColor( 1.0f, 0.4f, 0.0f );
        material.setDiffuseColor( 0.3f, 0.3f, 0.3f);
        material.setSpecularColor( 0.5f, 0.5f, 0.5f );
        material.setShininess(50f);
        
        wyglad_chwytaka.setMaterial(material);
        
        TextureLoader loader = new TextureLoader("metal1.png",
                                "LUMINANCE", new Container());
        Texture texture = loader.getTexture();
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.MODULATE);

        wyglad_chwytaka.setTexture(texture);
        wyglad_chwytaka.setTextureAttributes(texAttr);
        
        return wyglad_chwytaka;
    }
    
    //-------------------------------------
    //           OBSŁUGA ZDARZEŃ
    //-------------------------------------
    //  przyciski sterujące z klawiatury
    //
    public void keyPressed(KeyEvent e){
        if (e.getKeyChar()=='w') {key_w=true;}
        if (e.getKeyChar()=='s') {key_s=true;}
        if (e.getKeyChar()=='a') {key_a=true;}
        if (e.getKeyChar()=='d') {key_d=true;}
        if (e.getKeyChar()=='q') {key_q=true;}
        if (e.getKeyChar()=='e') {key_e=true;}
        
        if (e.getKeyChar()=='r') {key_r=true;}
        if (e.getKeyChar()=='f') {key_f=true;}
    }
    
    public void keyReleased(KeyEvent e){
        if (e.getKeyChar()=='w') {key_w=false;}
        if (e.getKeyChar()=='s') {key_s=false;}
        if (e.getKeyChar()=='a') {key_a=false;}
        if (e.getKeyChar()=='d') {key_d=false;}
        if (e.getKeyChar()=='q') {key_q=false;}
        if (e.getKeyChar()=='e') {key_e=false;}
        
        if (e.getKeyChar()=='r') {key_r=false;}
        if (e.getKeyChar()=='f') {key_f=false;}
    }
    
    public void keyTyped(KeyEvent e){ }
    
    //  obsluga przyciskow w oknie programu
    //
    public void actionPerformed(ActionEvent e){
        //  przycisk startujący
        if (e.getSource()==start_button){
            if(!timer.isRunning()){
                start_button.setLabel("Stop");
                start_button.setBackground(Color.red);
                timer.start();
                
                //  ustawienie dostępności pozostałych przycisków
                save_button.setEnabled(true);
                read_button.setEnabled(true);
                clear_button.setEnabled(true);
            }
            else{
                start_button.setLabel("Start");
                start_button.setBackground(Color.green);
                timer.stop();
                
                save_button.setEnabled(false);
                read_button.setEnabled(false);
                clear_button.setEnabled(false);
            }           
        }
        else {    
            ruch();         //  funkcja odpowiedzialna za poruszanie robotem
            chwytanie();    //  funkcja odpowiedzialna za łapanie kostki
            opuszczenie(spadanie,pozycja_spadania); //  funkcja z płynnym opadaiem kostki
        }
        
        // przycisk zapisujący
        if (e.getSource()==save_button){
            if(!zapis_flaga){
                save_button.setLabel("Skoncz");
                save_button.setBackground(Color.red);
                zapis_flaga = true;
                
                //  przypisanie do zmiennych nowego punkt startowego
                ostatnia_dlugosc = dlugosc;
                ostatnia_wysokosc = wysokosc;
                ostatni_kat = kat;
                kostka.getLocalToVworld(ostatnie_polozenie_kostki); 
                ostatnie_polozenie_kostki.get(ostatnia_pozycja);
                
                start_button.setEnabled(false);
                read_button.setEnabled(false);
                clear_button.setEnabled(false);
            }
            else{
                save_button.setLabel("Zapisz");
                save_button.setBackground(Color.green);
                zapis_flaga = false;
                    
                zapis_ruchu = pomocnicza_ruchu;
                zapis_ruchu = 0;
                
                start_button.setEnabled(true);
                read_button.setEnabled(true);
                clear_button.setEnabled(true);
            }
        }
        
        //  przycisk odtwarzający
        if (e.getSource()==read_button){
            if(!odczyt_flaga){
                read_button.setLabel("Zatrzymaj");
                read_button.setBackground(Color.red);
                odczyt_flaga = true;
                
                //  ustawienie robota do nowego punktu startowego
                ostatnie_polozenie_kostki.setTranslation(ostatnia_pozycja);
                pozycja_kostki.setTransform(ostatnie_polozenie_kostki);
                
                rotacja_ram.rotY(ostatni_kat);
                rotacja_ramienia.setTransform(rotacja_ram);
                
                ruch_ram.setTranslation(new Vector3f(0.0f, ostatnia_wysokosc, 0.05f));
                pozycja_ramienia.setTransform(ruch_ram);
         
                ruch_wys.setTranslation(new Vector3f(0.0f, ostatnia_dlugosc, 0.0f));
                pozycja_wysiegnika.setTransform(ruch_wys);
                
                kat = ostatni_kat;
                dlugosc = ostatnia_dlugosc;
                wysokosc = ostatnia_wysokosc;
                
                start_button.setEnabled(false);
                save_button.setEnabled(false);
                clear_button.setEnabled(false);
            }
            else{
                read_button.setLabel("Odtworz");
                read_button.setBackground(Color.green);
                odczyt_flaga = false;
                    
                pomocnicza_ruchu = 0;
                odczyt_ruchu = 0;
                
                start_button.setEnabled(true);
                save_button.setEnabled(true);
                clear_button.setEnabled(true);
            }
        }
        
        //  przycisk resetujący
        if(e.getSource()==clear_button){
            ostatnia_dlugosc = dlugosc;
            ostatnia_wysokosc = wysokosc;
            ostatni_kat = kat;
            
            kostka.getLocalToVworld(ostatnie_polozenie_kostki); 
            ostatnie_polozenie_kostki.get(ostatnia_pozycja);
            
            for(int i=0; i<7; i++) for(int j=0; j<12000; j++) ruchy[i][j] = false;
        }
        
    }
    
    //  reakcje programu na wciścnięcie przycisków
    //
    public void ruch(){
        odczyt_zapis();
        
        if(key_w && wysokosc<0.27f){wysokosc+=0.01f; }
        if(key_s && wysokosc>-0.25f){wysokosc-=0.01f;}
        
        if(key_a && kat<3.1f){kat+=0.03;}
        if(key_d && kat>-3.1f){kat-=0.03;}
        
        if(key_q && dlugosc<0.18f){dlugosc+=0.01f;}
        if(key_e && dlugosc>-0.18f){dlugosc-=0.01f;}
        
        if(key_f) {     //  resetowanie ustawienia kamery
            przesuniecie_obserwatora.set(new Vector3f(0.0f, 0.6f, 3.0f));
            Transform3D rot_obs = new Transform3D();
            rot_obs.rotX((float)(-Math.PI/16));
            przesuniecie_obserwatora.mul(rot_obs);
            universe.getViewingPlatform().getViewPlatformTransform().setTransform(przesuniecie_obserwatora);
        }

        rotacja_ram.rotY(kat);
        rotacja_ramienia.setTransform(rotacja_ram);
        
        ruch_ram.setTranslation(new Vector3f(0.0f, wysokosc, 0.05f));
        pozycja_ramienia.setTransform(ruch_ram);
         
        ruch_wys.setTranslation(new Vector3f(0.0f, dlugosc, 0.0f));
        pozycja_wysiegnika.setTransform(ruch_wys);
    }
    
    //  przenoszenie kostki
    //
    public void chwytanie(){
        //  kostka wolna
        if(key_r && kolizja.flaga && !flaga_schwytania) {
            graf_sceny.removeChild(bran_kostki);
                 
            Transform3D pozycja_5 = new Transform3D();    
            
            pozycja_5.setTranslation(new Vector3f(0.0f,0.33f,0));
            pozycja_kostki.setTransform(pozycja_5);
            
            pozycja_wysiegnika.addChild(bran_kostki);

            key_r=false;
            flaga_schwytania = true;
        }
        //  kostka już złapana
        if(key_r && flaga_schwytania) {           
            pozycja_wysiegnika.removeChild(bran_kostki);
            
            // ustawienie płynnego opuszczania kostki
            flaga_opuszczenia = true;
            
            wysiegnik_2.getLocalToVworld(spadanie); 
            spadanie.get(pozycja_spadania);
            
            pozycja_spadania.x -= (float)(cos(kat)*0.08f);
            pozycja_spadania.z += (float)(sin(kat)*0.08f);
            
            graf_sceny.addChild(bran_kostki);
            
            key_r=false;
            flaga_schwytania = false;
        }
    }
    
    //  funkcja odpowiedzialna za płynne opuszczanie kostki
    //
    public void opuszczenie(Transform3D spadanie, Vector3f pozycja_spadania){
        if(flaga_opuszczenia){
            if(pozycja_spadania.y<=-0.24f) flaga_opuszczenia = false;
            else pozycja_spadania.y -= 0.01f;
            
            spadanie.setTranslation(pozycja_spadania);
            pozycja_kostki.setTransform(spadanie);
        }
    }
    
    //  odczytywanie lub zapisywanie ruchów robota
    //
    public void odczyt_zapis(){
        if(zapis_flaga){
           if(key_w) ruchy[0][zapis_ruchu] = true;
           if(key_s) ruchy[1][zapis_ruchu] = true;
           if(key_a) ruchy[2][zapis_ruchu] = true;
           if(key_d) ruchy[3][zapis_ruchu] = true;
           if(key_q) ruchy[4][zapis_ruchu] = true;
           if(key_e) ruchy[5][zapis_ruchu] = true;
           if(key_r) ruchy[6][zapis_ruchu] = true;
           
            zapis_ruchu++;
            if(zapis_ruchu == 12000) zapis_flaga = false;
        }
        
        if(odczyt_flaga){   
            key_w = ruchy[0][odczyt_ruchu];
            key_s = ruchy[1][odczyt_ruchu];
            key_a = ruchy[2][odczyt_ruchu];
            key_d = ruchy[3][odczyt_ruchu];
            key_q = ruchy[4][odczyt_ruchu];
            key_e = ruchy[5][odczyt_ruchu];
            key_r = ruchy[6][odczyt_ruchu];
            
            odczyt_ruchu++;
            if(odczyt_ruchu == pomocnicza_ruchu) odczyt_flaga = false;
        }
    }
    
    public static void main(String[] args){
        ramie_robota robo = new ramie_robota();
        robo.addKeyListener(robo);
        MainFrame MF = new MainFrame(robo, 1000, 1000);        
    }
}