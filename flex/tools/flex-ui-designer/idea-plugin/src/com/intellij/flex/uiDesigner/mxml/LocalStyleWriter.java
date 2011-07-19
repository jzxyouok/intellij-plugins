package com.intellij.flex.uiDesigner.mxml;

import com.intellij.flex.uiDesigner.CssWriter;
import com.intellij.flex.uiDesigner.FlexUIDesignerBundle;
import com.intellij.flex.uiDesigner.InjectionUtil;
import com.intellij.flex.uiDesigner.ProblemsHolder;
import com.intellij.flex.uiDesigner.io.StringRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.intellij.psi.css.CssFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LocalStyleWriter {
  private byte[] data;
  private final CssWriter cssWriter;
  private final ProblemsHolder problemsHolder;

  public LocalStyleWriter(StringRegistry.StringWriter stringWriter, ProblemsHolder problemsHolder) {
    this.problemsHolder = problemsHolder;
    cssWriter = new CssWriter(stringWriter);
  }

  public
  @NotNull
  byte[] getData() {
    return data;
  }

  public boolean write(XmlTag tag, Module module) {
    data = null;

    CssFile cssFile = null;
    XmlAttribute source = tag.getAttribute("source");
    if (source != null) {
      XmlAttributeValue valueElement = source.getValueElement();
      if (valueElement != null) {
        final PsiFileSystemItem psiFile = InjectionUtil.getReferencedPsiFile(valueElement, problemsHolder, true);
        if (psiFile != null) {
          if (psiFile instanceof CssFile) {
            cssFile = (CssFile)psiFile;
          }
          else {
            problemsHolder.add(FlexUIDesignerBundle.message("error.embed.source.is.not.css.file", psiFile.getName()));
          }
        }
      }
    }
    else {
      PsiElement host = XmlTagValueProvider.getInjectedHost(tag);
      if (host != null) {
        InjectedPsiVisitor visitor = new InjectedPsiVisitor(host);
        InjectedLanguageUtil.enumerate(host, visitor);
        cssFile = visitor.getCssFile();
      }
    }

    if (cssFile == null) {
      return false;
    }

    data = cssWriter.write(cssFile, module, problemsHolder);
    return true;
  }

  private static class InjectedPsiVisitor implements PsiLanguageInjectionHost.InjectedPsiVisitor {
    private final PsiElement host;
    private boolean visited;

    private CssFile cssFile;

    public InjectedPsiVisitor(PsiElement host) {
      this.host = host;
    }

    public
    @Nullable
    CssFile getCssFile() {
      return cssFile;
    }

    public void visit(@NotNull PsiFile injectedPsi, @NotNull List<PsiLanguageInjectionHost.Shred> places) {
      assert !visited;
      visited = true;

      assert places.size() == 1;
      assert places.get(0).host == host;
      cssFile = (CssFile)injectedPsi;
    }
  }
}
