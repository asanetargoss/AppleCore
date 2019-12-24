package squeek.applecore.asm.module;

import org.objectweb.asm.tree.*;
import squeek.applecore.asm.ASMConstants;
import squeek.applecore.asm.IClassTransformerModule;
import squeek.asmhelper.applecore.ASMHelper;
import squeek.asmhelper.applecore.ObfHelper;
import squeek.asmhelper.applecore.ObfuscatedName;

import static org.objectweb.asm.Opcodes.*;

public class ModuleFoodEatingSpeed implements IClassTransformerModule
{
	private static final ObfuscatedName SET_ACTIVE_HAND = new ObfuscatedName("func_184598_c" /*setActiveHand*/);
	private static final ObfuscatedName GET_ITEM_IN_USE_MAX_COUNT = new ObfuscatedName("func_184612_cw" /*getItemInUseMaxCount*/);
	private static final ObfuscatedName TRANSFORM_EAT_FIRST_PERSON = new ObfuscatedName("func_187454_a" /*transformEatFirstPerson*/);
	private static final ObfuscatedName GET_MAX_ITEM_USE_DURATION = new ObfuscatedName("func_77988_m" /*getMaxItemUseDuration*/);
	
	private static final ObfuscatedName MC = new ObfuscatedName("field_78455_a" /*mc*/);
	private static final ObfuscatedName THE_PLAYER = new ObfuscatedName("field_71439_g" /*thePlayer*/);
	private static final ObfuscatedName ACTIVE_ITEM_STACK = new ObfuscatedName("field_184627_bm" /*activeItemStack*/);
	private static final ObfuscatedName ACTIVE_ITEM_STACK_USE_COUNT = new ObfuscatedName("field_184628_bn" /*activeItemStackUseCount*/);
	
	@Override
	public String[] getClassesToTransform()
	{
		return new String[]{
		ASMConstants.ENTITY_LIVING,
		ASMConstants.ITEM_RENDERER
		};
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		ClassNode classNode = ASMHelper.readClassFromBytes(basicClass);

		if (transformedName.equals(ASMConstants.ENTITY_LIVING))
		{
			addItemInUseMaxDurationField(classNode);

			MethodNode methodNode = ASMHelper.findMethodNodeOfClass(classNode, SET_ACTIVE_HAND.get(), ASMHelper.toMethodDescriptor("V", ASMConstants.HAND));
			if (methodNode != null)
			{
				patchSetActiveHand(classNode, methodNode);
			}
			else
				throw new RuntimeException(classNode.name + ": setActiveHand method not found");

			methodNode = ASMHelper.findMethodNodeOfClass(classNode, GET_ITEM_IN_USE_MAX_COUNT.get(), ASMHelper.toMethodDescriptor("I"));
			if (methodNode != null)
			{
				patchGetItemInUseMaxCount(classNode, methodNode);
			}
			else
				throw new RuntimeException(classNode.name + ": getItemInUseMaxCount method not found");
		}
		else if (transformedName.equals(ASMConstants.ITEM_RENDERER))
		{
			MethodNode methodNode = ASMHelper.findMethodNodeOfClass(classNode, TRANSFORM_EAT_FIRST_PERSON.get(), ASMHelper.toMethodDescriptor("V", "F", ASMConstants.HAND_SIDE, ASMConstants.STACK));
			if (methodNode != null)
			{
				patchRenderItemInFirstPerson(methodNode);
			}
			else
				throw new RuntimeException(classNode.name + ": setActiveHand method not found");
		}

		return ASMHelper.writeClassToBytes(classNode);
	}

	private void patchRenderItemInFirstPerson(MethodNode method)
	{
		InsnList needle = new InsnList();
		needle.add(new VarInsnNode(ALOAD, 3));
		needle.add(new MethodInsnNode(INVOKEVIRTUAL, ASMHelper.toInternalClassName(ASMConstants.STACK), GET_MAX_ITEM_USE_DURATION.get(), ASMHelper.toMethodDescriptor("I"), false));

		InsnList replacement = new InsnList();
		replacement.add(new VarInsnNode(ALOAD, 0));
		replacement.add(new FieldInsnNode(GETFIELD, ObfHelper.getInternalClassName(ASMConstants.ITEM_RENDERER), MC.get(), ASMHelper.toDescriptor(ASMConstants.MINECRAFT)));
		replacement.add(new FieldInsnNode(GETFIELD, ObfHelper.getInternalClassName(ASMConstants.MINECRAFT), THE_PLAYER.get(), ASMHelper.toDescriptor(ASMConstants.PLAYER_SP)));
		replacement.add(new FieldInsnNode(GETFIELD, ObfHelper.getInternalClassName(ASMConstants.PLAYER), "itemInUseMaxDuration", "I"));

		boolean replaced = ASMHelper.findAndReplace(method.instructions, needle, replacement) != null;
		if (!replaced)
			throw new RuntimeException("ItemRenderer.transformEatFirstPerson: no replacements made");
	}

	private void patchGetItemInUseMaxCount(ClassNode classNode, MethodNode method)
	{
		InsnList needle = new InsnList();
		needle.add(new VarInsnNode(ALOAD, 0));
		needle.add(new FieldInsnNode(GETFIELD, ObfHelper.getInternalClassName(ASMConstants.ENTITY_LIVING), ACTIVE_ITEM_STACK.get(), ASMHelper.toDescriptor(ASMConstants.STACK)));
		needle.add(new MethodInsnNode(INVOKEVIRTUAL, ObfHelper.getInternalClassName(ASMConstants.STACK), GET_MAX_ITEM_USE_DURATION.get(), ASMHelper.toMethodDescriptor("I"), false));

		InsnList replacement = new InsnList();
		replacement.add(new VarInsnNode(ALOAD, 0));
		replacement.add(new VarInsnNode(ALOAD, 0));
		replacement.add(new FieldInsnNode(GETFIELD, ASMHelper.toInternalClassName(classNode.name), "itemInUseMaxDuration", "I"));
		replacement.add(new MethodInsnNode(INVOKESTATIC, ASMHelper.toInternalClassName(ASMConstants.HOOKS), "getItemInUseMaxCount", ASMHelper.toMethodDescriptor("I", ASMConstants.ENTITY_LIVING, "I"), false));

		int numReplacementsMade = ASMHelper.findAndReplaceAll(method.instructions, needle, replacement);
		if (numReplacementsMade == 0)
			throw new RuntimeException("EntityLivingBase.getItemInUseMaxCount: no replacements made");
	}

	private void patchSetActiveHand(ClassNode classNode, MethodNode method)
	{
		AbstractInsnNode targetNode = ASMHelper.findFirstInstructionWithOpcode(method, PUTFIELD);
		while (targetNode != null && !((FieldInsnNode) targetNode).name.equals(ACTIVE_ITEM_STACK_USE_COUNT.get()))
		{
			targetNode = ASMHelper.findNextInstructionWithOpcode(targetNode, PUTFIELD);
		}

		if (targetNode == null)
			throw new RuntimeException("EntityLivingBase.setActiveHand: PUTFIELD activeItemStackUseCount instruction not found");

		InsnList toInject = new InsnList();

		toInject.add(new VarInsnNode(ALOAD, 0));
		toInject.add(new VarInsnNode(ILOAD, 3));
		toInject.add(new FieldInsnNode(PUTFIELD, ASMHelper.toInternalClassName(classNode.name), "itemInUseMaxDuration", "I"));

		method.instructions.insert(targetNode, toInject);
	}

	private void addItemInUseMaxDurationField(ClassNode classNode)
	{
		classNode.fields.add(new FieldNode(ACC_PUBLIC, "itemInUseMaxDuration", "I", null, null));
	}
}