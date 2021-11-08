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

    //set step sensor rate in seconds

    //read historical step count --> added to steps

    public static var historyStepCount:Int;
    public static var stepDeltaAccumulator:Int;

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

    ///Android Callbacks
    #if android
    public function onTrace(tag:String, msg:String)
    {
        trace(tag + ": " + msg);
    }

    public function historyUpdated(steps:Int)
    {
        historyStepCount = steps;
    }

    public function deltaUpdated(steps:Int)
    {
        stepDeltaAccumulator += steps;
    }
    #end

    #if android 
    private static var funcInit:Dynamic;
    #end

    #if ios
    #end
}