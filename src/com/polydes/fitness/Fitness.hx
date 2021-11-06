package com.polydes.fitness;

#if android
import lime.system.JNI;
#end

import com.stencyl.Engine;
import com.stencyl.event.EventMaster;
import com.stencyl.event.StencylEvent;
import com.stencyl.behavior.Script;
import com.stencyl.behavior.TimedTask;

import lime.system.CFFI;

import openfl.events.EventDispatcher;
import openfl.events.Event;

import haxe.Json;

#if ios
@:buildXml('<include name="${haxelib:com.polydes.fitness}/project/Build.xml"/>')
//This is just here to prevent the otherwise indirectly referenced native code from bring stripped at link time.
@:cppFileCode('extern "C" int fitness_register_prims();void com_polydes_fitness_link(){fitness_register_prims();}')
#end
class Fitness
{
    //lastDayChecked
    //stepCountOnLastDayChecked

    //stepsTotal
    //using an int here gives us roughly 300 years of 18k steps/day
    //2,147,483,647 (2^31 - 1) is the max value of an int

    //16,777,217 (2^24 + 1) is the first integer an ieee754 32-bit float can't represent.
    //since stencyl uses floats, stepsUsed (as a user attribute) can only accurately increase up to 16,777,217, which would give us about 2.5 years of 18k steps/day.
    //4.5 years at 10k steps/day

    //stepsUsed

    //get steps taken since last check

    #if android
    //Used for Android callbacks from Java
    public function new()
    {
    }
    #end

    public static function initialize():Void 
    {
        #if ios
        
        #end    
        
        #if android
        if(funcInit == null)
        {
            funcInit = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "initialize", "(Lorg/haxe/lime/HaxeObject;)V", true);
        }
        
        var args = new Array<Dynamic>();
        args.push(new Fitness());
        funcInit(args);
        #end
    }

    public static function requestPermissions():Void
    {
        #if ios
        
        #end
        
        #if android
        if(funcRequestPermissions == null)
        {
            funcRequestPermissions = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "requestPermissions", "()V", true);
        }
        
        funcRequestPermissions([]);
        #end
    }

    public static function rescindPermissions():Void
    {
        #if ios
        
        #end
        
        #if android
        if(funcRescindPermissions == null)
        {
            funcRescindPermissions = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "rescindPermissions", "()V", true);
        }
        
        funcRescindPermissions([]);
        #end
    }

    public static function allPermissionsApproved():Bool
    {
        #if ios
        
        #end
        
        #if android
        if(funcAllPermissionsApproved == null)
        {
            funcAllPermissionsApproved = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "allPermissionsApproved", "()Z", true);
        }
        
        return funcAllPermissionsApproved([]);
        #end
    }
    
    public static function getSteps():Int
    {
        #if ios
        
        #end
        
        #if android
        if(funcGetSteps == null)
        {
            funcGetSteps = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "getSteps", "()I", true);
        }
        
        return funcGetSteps([]);
        #end
    }

    public static function recordSteps():Void
    {
        #if ios
        
        #end
        
        #if android
        if(funcRecordSteps == null)
        {
            funcRecordSteps = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "recordSteps", "()V", true);
        }
        
        funcRecordSteps([]);
        #end
    }

    public static function updateSteps():Void
    {
        #if ios
        
        #end
        
        #if android
        if(funcUpdateSteps == null)
        {
            funcUpdateSteps = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "updateSteps", "()V", true);
        }
        
        funcUpdateSteps([]);
        #end
    }

    #if android 
    private static var funcInit:Dynamic;
    private static var funcRequestPermissions:Dynamic;
    private static var funcRescindPermissions:Dynamic;
    private static var funcGetSteps:Dynamic;
    private static var funcRecordSteps:Dynamic;
    private static var funcUpdateSteps:Dynamic;
    private static var funcAllPermissionsApproved:Dynamic;
    #end

    #if ios
    #end
}