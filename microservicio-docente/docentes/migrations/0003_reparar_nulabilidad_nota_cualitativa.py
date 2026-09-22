from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ("docentes", "0002_auditoria_academica"),
    ]

    operations = [
        migrations.RunSQL(
            sql="""
                ALTER TABLE "sga_docente"."calificaciones"
                ALTER COLUMN "nota_cualitativa" DROP NOT NULL;
            """,
            reverse_sql="""
                ALTER TABLE "sga_docente"."calificaciones"
                ALTER COLUMN "nota_cualitativa" SET NOT NULL;
            """,
        ),
    ]
