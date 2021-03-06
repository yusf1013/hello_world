package rendererEngine;

import dataHandler.ModelLoader;
import inputHandler.KeyboardHandler;
import javafx.scene.paint.Color;
import mathHandler.ThreeDObjectTransformations;
import mathHandler.VectorGeometry;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.newdawn.slick.TrueTypeFont;
import physicsEngine.CollisionModule.Collider;
import rendererEngine.itemBag.ItemBag;
import rendererEngine.scriptManager.Control;
import rendererEngine.scriptManager.MasterScript;
import threeDItems.*;
import org.lwjgl.stb.STBEasyFont.*;

import java.awt.*;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;



public class DisplayDriverGL extends VectorGeometry {

    //Mesh meshCube = new Mesh();
    //Vector<Mesh> meshVector = new Vector<>();
    //Vector<Mesh> meshVector;
    Matrix4by4 matProj;
    float screenHeight=600, screenWidth=600, walkSpeed=20f;
    Camera camera;
    ThreeDObjectTransformations trans = new ThreeDObjectTransformations();
    Vec3d light_direction = new Vec3d(0,0,-1);
    long window;
    List<Triangle> triToRaster = new Vector<>();
    private GLFWKeyCallback keyCallback;
    double cursorX, cursorY, mouseSensitivity=0.5f;
    MasterScript ms;
    Object arr[];
    Collider collider = new Collider();


    public DisplayDriverGL(long window)
    {
        ModelLoader modelLoader = new ModelLoader();
        //meshVector=modelLoader.loadAll();
        ItemBag.addMesh(modelLoader.loadAll());
        //ItemBag.camera = new Camera();
        camera=ItemBag.camera;

        ms=new MasterScript(this);

        matProj = makeProjectionMatrix(90.0f, (float)screenHeight / (float)screenWidth, 0.1f, 1000.0f);
        glfwSetKeyCallback(window, keyCallback = new KeyboardHandler(window));
        this.window=window;

        DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xBuffer, yBuffer);
        cursorX = xBuffer.get(0);
        cursorY = yBuffer.get(0);
        ms.run();

    }


    boolean onUserUpdate(float fElapsedTime)
    {
        /*if(ItemBag.gameOver)
        {
            handleUserInputs(fElapsedTime);
            return false;
        }*/
        //ms.run();
        calcMesh(fElapsedTime);
        draw();
        handleUserInputs(fElapsedTime);
        if(!ItemBag.gameOver)
            ms.run();
        return true;
    }


    public void calcMesh(float fElapsedTime)
    {
        Mesh mesh, colMesh;
        int size;
        if(ItemBag.modified)
        {
            ItemBag.modified = false;
            arr = ItemBag.getMeshMap().values().toArray();
        }
        size=arr.length;

        for(int i=0; i<size; i++)
        {
            mesh=(Mesh)arr[i];
            if(mesh.isRigidBody) {
                mesh.obb.getMesh(mesh);
                if(!mesh.flagNew) {
                    for(int j=0; j<size; j++)
                    {
                        if(i==j)
                            continue;

                        colMesh = (Mesh)arr[j];
                        collider.detectCollision(colMesh, mesh);

                    }
                }
                mesh.flagNew=true;
            }
            mesh.updateOldValaues();
            drawMesh(mesh, fElapsedTime);
        }
    }





    public void draw()
    {
        for(int i=0; i<triToRaster.size(); i++)
        {
            /*fillTriangle(triToRaster.get(i).p[0].x, triToRaster.get(i).p[0].y,
                    triToRaster.get(i).p[1].x, triToRaster.get(i).p[1].y,
                    triToRaster.get(i).p[2].x, triToRaster.get(i).p[2].y,  triToRaster.get(i).getColor());
            fillTriangle(triToRaster.get(i).p[0].x, triToRaster.get(i).p[0].y, triToRaster.get(i).p[0].z,
                    triToRaster.get(i).p[1].x, triToRaster.get(i).p[1].y, triToRaster.get(i).p[1].z,
                    triToRaster.get(i).p[2].x, triToRaster.get(i).p[2].y, triToRaster.get(i).p[2].z, triToRaster.get(i).getColor());
        */
            fillTriangle(triToRaster.get(i).p[0].x, triToRaster.get(i).p[0].y, triToRaster.get(i).p[0].z,
                    triToRaster.get(i).p[1].x, triToRaster.get(i).p[1].y, triToRaster.get(i).p[1].z,
                    triToRaster.get(i).p[2].x, triToRaster.get(i).p[2].y, triToRaster.get(i).p[2].z, triToRaster.get(i).getColor());
        }
        triToRaster.clear();
    }

    public static double getCursorPosX(long windowID) {
        DoubleBuffer posX = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(windowID, posX, null);
        return posX.get(0);
    }

    public static double getCursorPosY(long windowID) {
        DoubleBuffer posY = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(windowID, null, posY);
        return posY.get(0);
    }

    public void handleUserInputs(float fElapsedTime)
    {
        if(KeyboardHandler.isKeyDown(GLFW_KEY_ESCAPE))
        {
            glfwSetWindowShouldClose(window, true);
            KeyboardHandler.keys[GLFW_KEY_ESCAPE]=false;
            //ItemBag.keys[GLFW_KEY_ESCAPE]=false;
        }
    }

    public void cameraControls(float fElapsedTime)
    {
        if(KeyboardHandler.isKeyDown(GLFW_KEY_UP))
            camera.position.y+=walkSpeed*fElapsedTime;
        if(KeyboardHandler.isKeyDown(GLFW_KEY_DOWN))
            camera.position.y-=walkSpeed*fElapsedTime;
        if(KeyboardHandler.isKeyDown(GLFW_KEY_A))
            camera.position=vectorAdd(camera.position, vectorMul(normaliseVector(crossProduct(camera.vLookDir, camera.vUp)), fElapsedTime*walkSpeed));
        if(KeyboardHandler.isKeyDown(GLFW_KEY_D))
            camera.position=vectorSub(camera.position, vectorMul(normaliseVector(crossProduct(camera.vLookDir, camera.vUp)), fElapsedTime*walkSpeed));
        Vec3d vForward = vectorMul(camera.vLookDir, walkSpeed * fElapsedTime);
        if(KeyboardHandler.isKeyDown(GLFW_KEY_W))
            camera.position=vectorAdd(camera.position, vForward);
        if(KeyboardHandler.isKeyDown(GLFW_KEY_S))
            camera.position=vectorSub(camera.position, vForward);
       /* if(KeyboardHandler.isKeyDown(GLFW_KEY_LEFT))
            camera.setYaw(camera.getYaw() - walkSpeed/4f*fElapsedTime);
        if(KeyboardHandler.isKeyDown(GLFW_KEY_RIGHT))
            camera.setYaw(camera.getYaw() + walkSpeed/4f*fElapsedTime);*/
        if(KeyboardHandler.isKeyDown(GLFW_KEY_R))
            camera.setPitch(camera.getPitch() - walkSpeed/4f*fElapsedTime);
        if(KeyboardHandler.isKeyDown(GLFW_KEY_F))
            camera.setPitch(camera.getPitch() + walkSpeed/4f*fElapsedTime);

        double newCursorY=getCursorPosY(window), newCursorX=getCursorPosX(window);
        if(cursorY != newCursorY || cursorX!= newCursorX ) {
            double yDifference = (cursorY - newCursorY) / (double) MainGL.HEIGHT;
            double xDifference = (cursorX - newCursorX) / 400d;
            if((camera.getPitch() - mouseSensitivity * yDifference) <3.14/2f && (camera.getPitch() - mouseSensitivity * yDifference) >-3.14/2f)
                camera.setPitch(camera.getPitch() - (float)mouseSensitivity * (float)yDifference);
            camera.setYaw(camera.getYaw() - (float)mouseSensitivity * (float)xDifference);

        }
        if(!(newCursorY >0 && newCursorY<MainGL.HEIGHT && newCursorX>1 && newCursorX<MainGL.WIDTH)) {
            /*glfwSetCursorPos(window, MainGL.WIDTH/2, MainGL.HEIGHT/2);
            cursorY=MainGL.HEIGHT/2;
            cursorX=MainGL.WIDTH/2;*/
            cursorY=MainGL.HEIGHT/2;
            cursorX=MainGL.WIDTH/2;
            glfwSetCursorPos(window, cursorX, cursorY);
            /*cursorY=newCursorY-10;
            cursorX=newCursorX-10;*/
        }
        else
        {
            cursorY = newCursorY;
            cursorX=newCursorX;
        }
    }

    public boolean drawMesh(Mesh meshCube, float fElapsedTime)
    {
        Matrix4by4 matWorld = meshCube.getWorldMat();
        Matrix4by4 matView = camera.createViewMat();
        for (int i=0; i<meshCube.tris.size(); i++)
        {
            Triangle triTranslated=new Triangle(), triProjected = new Triangle();
            triTranslated=trans.transform(matWorld, meshCube.tris.get(i));

            Vec3d normal, line1, line2;
            line1=vectorSub(triTranslated.p[1], triTranslated.p[0]);
            line2=vectorSub(triTranslated.p[2], triTranslated.p[0]);
            normal = normaliseVector(crossProduct(line1, line2));

            // Get Ray from triangle to camera
            Vec3d vCameraRay = vectorSub(triTranslated.p[0], camera.position);

            //if (dotProduct(normal, vCameraRay) < 0.0f)
            {
                Triangle arr[] = null;
                triTranslated= trans.transform(matView, triTranslated);

                arr=zClip(triTranslated, i);

                if(arr==null || arr.length==0)
                    continue;
                /*arr=clip(arr);

                if(arr==null || arr.length==0)
                    continue;*/

                for(int mor=0; mor<arr.length; mor++)
                {
                    triToRaster.add(projectTriangle(arr[mor], normal));
                }
                //ystem.out.println("fixie");
            }
        }
        return true;
    }

    public Triangle projectTriangle(Triangle triTranslated, Vec3d normal)
    {
        Triangle triProjected=trans.transform(matProj, triTranslated);
        triProjected.p[0] = vectorDiv(triProjected.p[0], triProjected.p[0].w);
        triProjected.p[1] = vectorDiv(triProjected.p[1], triProjected.p[1].w);
        triProjected.p[2] = vectorDiv(triProjected.p[2], triProjected.p[2].w);
        Color color = triProjected.getColor();

        if(ItemBag.lightMode==1)
            light_direction=new Vec3d(3.0f, 3.40f, 3.72f);

        light_direction = normaliseVector(light_direction);
        float dp2, dp = Math.max(0.1f, (float) Math.abs((double) dotProduct(light_direction, normal)));

        if(ItemBag.lightMode==2)
        {
            dp = Math.max(0.1f, (float) Math.abs((double) dotProduct(normaliseVector(camera.vLookDir), normal)));
        }
        else if(ItemBag.lightMode==3)
        {
            dp2 = Math.max(0.1f, (float) Math.abs((double) dotProduct(normaliseVector(camera.vLookDir), normal)));
            dp=Math.max(dp, dp2);
        }
        //float dp=1.0f;
        triProjected.setColor(new Color(color.getRed() * dp, color.getGreen() * dp, color.getBlue() * dp, color.getOpacity() * 1));
        return triProjected;
    }

    public Triangle[] zClip(Triangle tri, int indind)
    {
        Vector <Vec3d> vec = new Vector<>();
        vec.add(tri.p[0]);
        vec.add(tri.p[1]);
        vec.add(tri.p[2]);

        int count=0;

        if(tri.p[0].z<0.1)
            count++;
        if(tri.p[1].z<0.1)
            count++;
        if(tri.p[2].z<0.1)
            count++;
        if(count==3)
            return null;

        if(count==1)
        {
            int lowestOne;
            if(tri.p[0].z<tri.p[1].z)
                lowestOne=0;
            else
                lowestOne=1;
            if(tri.p[lowestOne].z>tri.p[2].z)
                lowestOne=2;
            Vec3d tempV=tri.p[lowestOne];
            vec.remove(lowestOne);
            float newX=zcalc(vec.elementAt(0).x, vec.elementAt(0).z, tempV.x, tempV.z);
            float newY=zcalc(vec.elementAt(0).y, vec.elementAt(0).z, tempV.y, tempV.z);
            Vec3d new2 = new Vec3d(newX, newY, 0.1f);
            newX=zcalc(vec.elementAt(1).x, vec.elementAt(1).z, tempV.x, tempV.z);
            newY=zcalc(vec.elementAt(1).y, vec.elementAt(1).z, tempV.y, tempV.z);
            Vec3d new3 = new Vec3d(newX, newY, 0.1f);
            vec.add(new2);
            vec.add(new3);
            Triangle triArr[] = new Triangle[2];
            triArr[0]=new Triangle(vec.elementAt(0), vec.elementAt(1), vec.elementAt(2));
            triArr[0].setColor(tri.getColor());
            triArr[1]=new Triangle(vec.elementAt(1), vec.elementAt(2), vec.elementAt(3));
            triArr[1].setColor(tri.getColor());
            return  triArr;
        }
        else if(count==2)
        {
            tri.p[0].z*=-1; tri.p[1].z*=-1; tri.p[2].z*=-1;
            int lowestOne;
            if(tri.p[0].z<tri.p[1].z)
                lowestOne=0;
            else
                lowestOne=1;
            if(tri.p[lowestOne].z>tri.p[2].z)
                lowestOne=2;
            tri.p[0].z*=-1; tri.p[1].z*=-1; tri.p[2].z*=-1;
            Vec3d tempV=tri.p[lowestOne];
            vec.remove(lowestOne);
            float newX=zcalc(vec.elementAt(0).x, vec.elementAt(0).z, tempV.x, tempV.z);
            float newY=zcalc(vec.elementAt(0).y, vec.elementAt(0).z, tempV.y, tempV.z);
            Vec3d new2 = new Vec3d(newX, newY, 0.1f);
            newX=zcalc(vec.elementAt(1).x, vec.elementAt(1).z, tempV.x, tempV.z);
            newY=zcalc(vec.elementAt(1).y, vec.elementAt(1).z, tempV.y, tempV.z);
            Vec3d new3 = new Vec3d(newX, newY, 0.1f);
            vec.add(lowestOne, new3);
            vec.add(lowestOne, new2);

            Vector<Vec3d> vec2= new Vector<>();
            vec2.add(new2);
            vec2.add(new3);
            vec2.add(lowestOne, tempV);

            Triangle triArr[] = new Triangle[1];
            triArr[0]= new Triangle(vec2.elementAt(0), vec2.elementAt(1), vec2.elementAt(2));
            triArr[0].setColor(tri.getColor());
            return  triArr;
        }
        Triangle arr[] = new Triangle[1];
        arr[0]=tri;
        return  arr;
    }

    public float zcalc(float xoy1, float z1, float xoy3, float z3)
    {
        if(z1==z3) {
           System.out.println("You died");
        }
        return (((xoy1-xoy3)*(0.1f-z3)/(z1-z3))+xoy3);
    }

    public static void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, Color fill)
    {

        glColor3d(fill.getRed(), fill.getGreen(), fill.getBlue());
        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x1, y1);
        glVertex2f(x2, y2);
        glVertex2f(x3, y3);

        glTexCoord2f(0, 0);
        glTexCoord2f(0.5f, 0);
        glTexCoord2f(0.5f, 0.5f);

        glEnd();
    }

    public static void fillTriangle(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, Color fill)
    {

        glColor3d(fill.getRed(), fill.getGreen(), fill.getBlue());
        //if(fill==Color.TRANSPARENT)
        //glColor3d(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue());
            //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glBegin(GL_TRIANGLE_STRIP);
        glVertex3f(x1, y1, z1);
        glVertex3f(x2, y2, z2);
        glVertex3f(x3, y3, z3);

        glTexCoord2f(x1, y1);
        glTexCoord2f(x2, y2);
        glTexCoord2f(x3, y3);
        glEnd();
    }



/*    public Triangle[] clip(Triangle [] tri)
    {
        ArrayList<Triangle> al = new ArrayList<>(), al2 = new ArrayList<>();

        for(Triangle t:tri){
            al.add(t);
        }

        for(Triangle t:al) {

            Triangle arr[] = zClip(swapXZ(t), 0);
            if(arr!=null)
            for(Triangle t2:arr) {
                al2.add(swapXZ(t2));
            }
        }
        al.addAll(al2);

        //return (Triangle[]) al.toArray();
        Triangle brr[] = new Triangle[al.size()];
        int i=0;
        for(Triangle t2: al){
            brr[i]=t2;
            i++;
        }
        return brr;

    }

    public Triangle swapXZ(Triangle tri)
    {
        for(int i=0; i<3; i++)
        {
            float f=tri.p[i].z;
            tri.p[i].z=tri.p[i].x;
            tri.p[i].x=f;
        }

        return tri;
    }*/

}
