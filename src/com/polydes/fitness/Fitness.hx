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
    public static var isInitialized:Bool;
    
    #if android
    //Used for Android callbacks from Java
    public function new()
    {
    }
    #end

    #if android
    static var jniFunctionMap:Map<String, Dynamic> = new Map<String, Dynamic>();
    private static var noArgs = [];
    #end

    public static function call(functionName:String, ?type:String = "()V", ?args:Array<Dynamic> = null):Void
    {
        #if ios
        
        #end
        
        #if android
        var jniFunction = jniFunctionMap.get(functionName);
        if(jniFunction == null)
        {
            jniFunction = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", functionName, type, true);
            jniFunctionMap.set(functionName, jniFunction);
        }

        jniFunction(args == null ? args : noArgs);
        #end
    }

    public static function get(functionName:String, type:String, ?args:Array<Dynamic> = null):Dynamic
    {
        #if ios
        
        #end
        
        #if android
        var jniFunction = jniFunctionMap.get(functionName);
        if(jniFunction == null)
        {
            jniFunction = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", functionName, type, true);
            jniFunctionMap.set(functionName, jniFunction);
        }
        
        return jniFunction(args == null ? args : noArgs);
        #end
    }

    public static function initialize():Void 
    {
        if(isInitialized)
        {
            return;
        }

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
        isInitialized = true;
        #end
    }

    #if android
    public static function trySubscribeToStepRecording():Void
    {
        if(func_trySubscribeToStepRecording == null)
        {
            func_trySubscribeToStepRecording = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "trySubscribeToStepRecording", "()V", true);
        }
        
        func_trySubscribeToStepRecording();
    }

    public static function tryReadStepHistoryData(startTimeSeconds:Int, endTimeSeconds:Int, callback:(Int)->Void):Void
    {
        if(func_tryReadStepHistoryData == null)
        {
            func_tryReadStepHistoryData = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "tryReadStepHistoryData", "(IILorg/haxe/lime/HaxeObject;)V", true);
        }

        var stepsTakenCallback = new FitnessStepCallback();
        stepsTakenCallback.stepsTaken = callback;

        func_tryReadStepHistoryData(([startTimeSeconds, endTimeSeconds, stepsTakenCallback]:Array<Dynamic>));
    }

    public static function tryRegisterStepSensorListener(samplingRate:Int, callback:(Int)->Void):Void
    {
        if(func_tryRegisterStepSensorListener == null)
        {
            func_tryRegisterStepSensorListener = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "tryRegisterStepSensorListener", "(ILorg/haxe/lime/HaxeObject;)V", true);
        }

        var stepsTakenCallback = new FitnessStepCallback();
        stepsTakenCallback.stepsTaken = callback;
        
        func_tryRegisterStepSensorListener(([samplingRate, stepsTakenCallback]:Array<Dynamic>));
    }

    public static function tryUnregisterStepSensorListener():Void
    {
        if(func_tryUnregisterStepSensorListener == null)
        {
            func_tryUnregisterStepSensorListener = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "tryUnregisterStepSensorListener", "()V", true);
        }
        
        func_tryUnregisterStepSensorListener();
    }

    public static function allPermissionsApproved():Bool
    {
        if(func_allPermissionsApproved == null)
        {
            func_allPermissionsApproved = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "allPermissionsApproved", "()Z", true);
        }
        
        return func_allPermissionsApproved();
    }

    public static function requestPermissions():Void
    {
        if(func_requestPermissions == null)
        {
            func_requestPermissions = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "requestPermissions", "()V", true);
        }
        
        func_requestPermissions();
    }

    public static function rescindPermissions():Void
    {
        if(func_rescindPermissions == null)
        {
            func_rescindPermissions = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "rescindPermissions", "()V", true);
        }
        
        func_rescindPermissions();
    }

    public static function currentTime():Int
    {
        if(func_currentTime == null)
        {
            func_currentTime = JNI.createStaticMethod("com/stencyl/fitness/AndroidFitness", "currentTime", "()I", true);
        }
        
        return func_currentTime();
    }

    #end

    ///Android Callbacks
    #if android
    public function onTrace(tag:String, msg:String)
    {
        trace(tag + ": " + msg);
    }
    #end

    #if android 
    private static var funcInit:Dynamic;
    private static var func_trySubscribeToStepRecording:Dynamic;
    private static var func_tryReadStepHistoryData:Dynamic;
    private static var func_tryRegisterStepSensorListener:Dynamic;
    private static var func_tryUnregisterStepSensorListener:Dynamic;
    private static var func_allPermissionsApproved:Dynamic;
    private static var func_requestPermissions:Dynamic;
    private static var func_rescindPermissions:Dynamic;
    private static var func_currentTime:Dynamic;
    #end

    #if ios
    #end
}

#if android
class FitnessStepCallback
{
    public function new() {}
    public dynamic function stepsTaken(steps:Int) {}
}
#end