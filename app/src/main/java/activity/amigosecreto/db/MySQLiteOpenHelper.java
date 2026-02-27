package activity.amigosecreto.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_GRUPO = "grupo";
    public static final String COLUMN_GRUPO_ID = "id";
    public static final String COLUMN_GRUPO_NOME = "nome";
    public static final String COLUMN_GRUPO_DATA = "data";

    public static final String TABLE_PARTICIPANTE = "participante";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOME = "nome";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_TELEFONE = "telefone";
    public static final String COLUMN_AMIGO_SORTEADO_ID = "amigo_sorteado_id";
    public static final String COLUMN_ENVIADO = "enviado";
    public static final String COLUMN_FK_GRUPO_ID = "grupo_id";

    public static final String TABLE_EXCLUSAO = "exclusao";
    public static final String COLUMN_PARTICIPANTE_ID = "participante_id";
    public static final String COLUMN_EXCLUIDO_ID = "excluido_id";

    // Mantendo colunas antigas para evitar erros de compilação em DAOs não utilizados
    public static final String TABLE_DESEJO = "desejo";
    public static final String COLUMN_PRODUTO = "produto";
    public static final String COLUMN_CATEGORIA = "categoria";
    public static final String COLUMN_PRECO_MINIMO = "preco_minimo";
    public static final String COLUMN_PRECO_MAXIMO = "preco_maximo";
    public static final String COLUMN_LOJAS = "lojas";
    public static final String COLUMN_DESEJO_PARTICIPANTE_ID = "participante_id";

    private static final String DATABASE_NAME = "amigosecreto_v8.db";
    private static final int DATABASE_VERSION = 8;

    public MySQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlGrupo = "CREATE TABLE " + TABLE_GRUPO + "("
                + COLUMN_GRUPO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_GRUPO_NOME + " TEXT NOT NULL, "
                + COLUMN_GRUPO_DATA + " TEXT"
                + ")";
        db.execSQL(sqlGrupo);

        String sqlParticipante = "CREATE TABLE " + TABLE_PARTICIPANTE + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NOME + " TEXT NOT NULL, "
                + COLUMN_EMAIL + " TEXT, "
                + COLUMN_TELEFONE + " TEXT, "
                + COLUMN_AMIGO_SORTEADO_ID + " INTEGER, "
                + COLUMN_ENVIADO + " INTEGER DEFAULT 0, "
                + COLUMN_FK_GRUPO_ID + " INTEGER, "
                + "FOREIGN KEY(" + COLUMN_FK_GRUPO_ID + ") REFERENCES " + TABLE_GRUPO + "(" + COLUMN_GRUPO_ID + ")"
                + ")";
        db.execSQL(sqlParticipante);

        String sqlExclusao = "CREATE TABLE " + TABLE_EXCLUSAO + "("
                + COLUMN_PARTICIPANTE_ID + " INTEGER, "
                + COLUMN_EXCLUIDO_ID + " INTEGER, "
                + "PRIMARY KEY (" + COLUMN_PARTICIPANTE_ID + ", " + COLUMN_EXCLUIDO_ID + ")"
                + ")";
        db.execSQL(sqlExclusao);

        String sqlDesejo = "CREATE TABLE " + TABLE_DESEJO + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PRODUTO + " TEXT NOT NULL, "
                + COLUMN_CATEGORIA + " TEXT, "
                + COLUMN_PRECO_MINIMO + " REAL, "
                + COLUMN_PRECO_MAXIMO + " REAL, "
                + COLUMN_LOJAS + " TEXT, "
                + COLUMN_DESEJO_PARTICIPANTE_ID + " INTEGER, "
                + "FOREIGN KEY(" + COLUMN_DESEJO_PARTICIPANTE_ID + ") REFERENCES " + TABLE_PARTICIPANTE + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(sqlDesejo);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 8) {
            // Adicionar coluna participante_id na tabela desejo
            db.execSQL("ALTER TABLE " + TABLE_DESEJO + " ADD COLUMN " + COLUMN_DESEJO_PARTICIPANTE_ID + " INTEGER");
        }

        // Se precisar recriar tudo (para versões muito antigas)
        if (oldVersion < 7) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRUPO);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARTICIPANTE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXCLUSAO);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DESEJO);
            onCreate(db);
        }
    }
}
